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
    public Agendamento criarAgendamento(CreateAgendamentoDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.CLIENTE);

        if (!dto.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível criar agendamentos no passado.");
        }

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        Prestador prestador = servico.getPrestador();
        int duracao = servico.getDuracaoMinutos();
        LocalDateTime inicio = dto.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(duracao);

        // Verifica se o prestador está disponível neste horário
        boolean disponivel = disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), inicio, duracao);
        if (!disponivel) {
            throw new IllegalArgumentException("O prestador não está disponível nesse horário.");
        }

        if (!prestador.getNegocio().isAtivo()) {
            throw new IllegalArgumentException("Negócio está inativo.");
        }

        // Verifica se há agendamento conflitante no mesmo intervalo
        List<Agendamento> agendamentos = agendamentoRepository.findByPrestadorId(prestador.getId());
        boolean conflita = agendamentos.stream().anyMatch(ag -> {
            LocalDateTime agInicio = ag.getDataHora();
            LocalDateTime agFim = agInicio.plusMinutes(ag.getServico().getDuracaoMinutos());
            return overlaps(inicio, fim, agInicio, agFim);
        });

        if (conflita) {
            throw new IllegalArgumentException("Horário indisponível para o serviço selecionado.");
        }

        // Cria o agendamento
        Agendamento agendamento = Agendamento.builder()
                .cliente((Cliente) usuario)
                .servico(servico)
                .prestador(prestador)
                .dataHora(dto.getDataHora())
                .status(StatusAgendamento.PENDENTE)
                .build();

        return agendamentoRepository.save(agendamento);
    }

    private boolean overlaps(LocalDateTime inicio1, LocalDateTime fim1, LocalDateTime inicio2, LocalDateTime fim2) {
        return !(fim1.isBefore(inicio2) || inicio1.isAfter(fim2) || fim1.equals(inicio2) || inicio1.equals(fim2));
    }

    @Transactional
    public Agendamento atualizarAgendamento(Integer agendamentoId, CreateAgendamentoDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();

        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        // Só o cliente dono, o prestador ou o admin podem alterar
        if (!agendamento.getCliente().getId().equals(usuario.getId()) &&
                !agendamento.getPrestador().getId().equals(usuario.getId()) &&
                !PermissaoUtils.isAdmin(usuario)) {
            throw new SecurityException("Sem permissão para alterar este agendamento.");
        }

        // Só permite alterar agendamentos que ainda não aconteceram
        if (!agendamento.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível alterar agendamentos que já ocorreram.");
        }

        //  Impede mover para o passado
        if (!dto.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível alterar agendamentos para o passado.");
        }


        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        Prestador prestador = servico.getPrestador();
        int duracao = servico.getDuracaoMinutos();
        LocalDateTime novoInicio = dto.getDataHora();
        LocalDateTime novoFim = novoInicio.plusMinutes(duracao);

        // Verifica disponibilidade
        boolean disponivel = disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), novoInicio, duracao);
        if (!disponivel) {
            throw new IllegalArgumentException("O prestador não está disponível nesse horário.");
        }

        // Verifica conflitos (ignora o próprio agendamento)
        List<Agendamento> agendamentos = agendamentoRepository.findByPrestadorId(prestador.getId());
        boolean conflita = agendamentos.stream()
                .filter(ag -> !ag.getId().equals(agendamentoId))
                .anyMatch(ag -> {
                    LocalDateTime agInicio = ag.getDataHora();
                    LocalDateTime agFim = agInicio.plusMinutes(ag.getServico().getDuracaoMinutos());
                    return overlaps(novoInicio, novoFim, agInicio, agFim);
                });

        if (conflita) {
            throw new IllegalArgumentException("Horário indisponível para o serviço selecionado.");
        }

        // Atualiza dados
        agendamento.setServico(servico);
        agendamento.setPrestador(prestador);
        agendamento.setDataHora(dto.getDataHora());
        agendamento.setStatus(StatusAgendamento.PENDENTE);

        return agendamentoRepository.save(agendamento);
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

