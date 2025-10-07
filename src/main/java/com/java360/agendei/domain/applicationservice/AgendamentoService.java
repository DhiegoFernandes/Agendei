package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.CreateAgendamentoDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeService disponibilidadeService;

    @Transactional
    public Agendamento salvarOuAtualizarAgendamento(CreateAgendamentoDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.CLIENTE);

        if (!dto.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível criar/alterar agendamentos no passado.");
        }

        Agendamento agendamento;
        Servico servico;

        if (dto.getIdAgendamento() != null) {
            // Atualização
            agendamento = agendamentoRepository.findById(dto.getIdAgendamento())
                    .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

            // Bloqueia alteração se agendamento estiver cancelado
            if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
                throw new IllegalArgumentException("Não é possível alterar um agendamento cancelado.");
            }

            // Valida permissão
            if (!agendamento.getCliente().getId().equals(usuario.getId()) &&
                    !agendamento.getPrestador().getId().equals(usuario.getId()) &&
                    !PermissaoUtils.isAdmin(usuario)) {
                throw new SecurityException("Sem permissão para alterar este agendamento.");
            }

            // Se nenhum servicoId foi enviado, usa o atual
            if (dto.getServicoId() == null) {
                servico = agendamento.getServico();
            } else {
                servico = servicoRepository.findById(dto.getServicoId())
                        .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));
            }

        } else {
            // Criação
            if (dto.getServicoId() == null)
                throw new IllegalArgumentException("O ID do serviço é obrigatório ao criar um agendamento.");

            servico = servicoRepository.findById(dto.getServicoId())
                    .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

            agendamento = Agendamento.builder()
                    .cliente((Cliente) usuario)
                    .servico(servico)
                    .prestador(servico.getPrestador())
                    .status(StatusAgendamento.PENDENTE)
                    .build();
        }

        // Valida disponibilidade
        Prestador prestador = servico.getPrestador();
        int duracao = servico.getDuracaoMinutos();
        LocalDateTime inicio = dto.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(duracao);

        boolean disponivel = disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), inicio, duracao);
        if (!disponivel) {
            throw new IllegalArgumentException("O prestador não está disponível nesse horário.");
        }

        // Atualiza horário
        agendamento.setDataHora(dto.getDataHora());
        agendamento.setServico(servico);
        agendamento.setPrestador(prestador);
        agendamento.setStatus(StatusAgendamento.PENDENTE);

        return agendamentoRepository.save(agendamento);
    }





    private boolean overlaps(LocalDateTime inicio1, LocalDateTime fim1, LocalDateTime inicio2, LocalDateTime fim2) {
        return !(fim1.isBefore(inicio2) || inicio1.isAfter(fim2) || fim1.equals(inicio2) || inicio1.equals(fim2));
    }

  

    @Transactional
    public void concluirAgendamento(Integer agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        Usuario usuario = UsuarioAutenticado.get();
        if (!agendamento.getPrestador().getId().equals(usuario.getId()) &&
                !PermissaoUtils.isAdmin(usuario)) {
            throw new SecurityException("Sem permissão para concluir este agendamento.");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
    }

    public List<Agendamento> listarAgendamentosCliente() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.CLIENTE);

        return agendamentoRepository.findByClienteId(usuario.getId());
    }

    public List<Agendamento> listarAgendamentosPrestador() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        return agendamentoRepository.findByPrestadorId(usuario.getId());
    }

    @Transactional
    public void cancelarAgendamento(Integer agendamentoId) {
        Usuario usuario = UsuarioAutenticado.get();
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        boolean isCliente = agendamento.getCliente().getId().equals(usuario.getId());
        boolean isPrestador = agendamento.getPrestador().getId().equals(usuario.getId());
        boolean isAdmin = PermissaoUtils.isAdmin(usuario);

        if (!isCliente && !isPrestador && !isAdmin) {
            throw new SecurityException("Você não tem permissão para cancelar este agendamento.");
        }

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new IllegalArgumentException("Apenas agendamentos pendentes podem ser cancelados.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
    }

}

