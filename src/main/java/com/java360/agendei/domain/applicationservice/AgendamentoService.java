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
import org.springframework.scheduling.annotation.Scheduled;
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

        // Verifica se o cliente já possui 4 agendamentos pendentes
        long agendamentosPendentes = agendamentoRepository.countByClienteIdAndStatus(
                usuario.getId(), StatusAgendamento.PENDENTE);

        if (agendamentosPendentes >= 4) {
            throw new IllegalArgumentException("Você já possui o limite máximo de 4 agendamentos ativos.");
        }

        if (dto.getServicoId() == null) {
            throw new IllegalArgumentException("O ID do serviço é obrigatório ao criar um agendamento.");
        }

        if (!dto.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível criar um agendamento no passado.");
        }

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        // Bloqueia agendamento em serviço inativo
        if (!servico.isAtivo()) {
            throw new IllegalArgumentException("Não é possível agendar um serviço inativo.");
        }

        Prestador prestador = servico.getPrestador();
        Negocio negocio = prestador.getNegocio();

        // Bloqueia agendamento se o negócio estiver inativo
        if (negocio == null || !negocio.isAtivo()) {
            throw new IllegalArgumentException("Não é possível agendar um serviço de um negócio inativo.");
        }

        int duracao = servico.getDuracaoMinutos();
        LocalDateTime inicio = dto.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(duracao);

        // Verifica disponibilidade do prestador
        if (!disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), inicio, duracao)) {
            throw new IllegalArgumentException("O prestador não está disponível nesse horário.");
        }

        // Verifica sobreposição de horários
        List<Agendamento> agendamentosExistentes = agendamentoRepository.findByPrestadorId(prestador.getId());
        boolean conflita = agendamentosExistentes.stream()
                .filter(ag -> ag.getStatus() == StatusAgendamento.PENDENTE) // ignora cancelados e concluídos
                .anyMatch(ag -> overlaps(
                        inicio, fim,
                        ag.getDataHora(),
                        ag.getDataHora().plusMinutes(ag.getServico().getDuracaoMinutos())
                ));

        if (conflita) {
            throw new IllegalArgumentException("Horário indisponível. Já existe um agendamento neste período.");
        }

        // Criação do agendamento
        Agendamento agendamento = Agendamento.builder()
                .cliente((Cliente) usuario)
                .prestador(prestador)
                .servico(servico)
                .dataHora(inicio)
                .status(StatusAgendamento.PENDENTE)
                .build();

        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento atualizarAgendamento(Integer id, CreateAgendamentoDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.CLIENTE, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        // Impede alterações em agendamentos cancelados
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalArgumentException("Não é possível alterar um agendamento cancelado.");
        }

        boolean isCliente = agendamento.getCliente().getId().equals(usuario.getId());
        boolean isPrestador = agendamento.getPrestador().getId().equals(usuario.getId());
        boolean isAdmin = PermissaoUtils.isAdmin(usuario);

        if (!isCliente && !isPrestador && !isAdmin) {
            throw new SecurityException("Você não tem permissão para alterar este agendamento.");
        }

        if (!dto.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível agendar para o passado.");
        }

        Servico servico = (dto.getServicoId() != null)
                ? servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."))
                : agendamento.getServico();

        // Bloqueia alteração para serviço inativo
        if (!servico.isAtivo()) {
            throw new IllegalArgumentException("Não é possível alterar para um serviço inativo.");
        }

        Prestador prestador = servico.getPrestador();
        Negocio negocio = prestador.getNegocio();

        // Bloqueia alteração se o negócio estiver inativo
        if (negocio == null || !negocio.isAtivo()) {
            throw new IllegalArgumentException("Não é possível alterar para um serviço de um negócio inativo.");
        }

        int duracao = servico.getDuracaoMinutos();
        LocalDateTime inicio = dto.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(duracao);

        // Verifica disponibilidade
        if (!disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), inicio, duracao)) {
            throw new IllegalArgumentException("O prestador não está disponível nesse horário.");
        }

        // Verifica sobreposição (ignora o próprio agendamento)
        List<Agendamento> agendamentosExistentes = agendamentoRepository.findByPrestadorId(prestador.getId());
        boolean conflita = agendamentosExistentes.stream()
                .filter(a -> !a.getId().equals(agendamento.getId()))
                .filter(a -> a.getStatus() == StatusAgendamento.PENDENTE) // só verifica pendentes
                .anyMatch(a -> overlaps(
                        inicio, fim,
                        a.getDataHora(),
                        a.getDataHora().plusMinutes(a.getServico().getDuracaoMinutos())
                ));

        if (conflita) {
            throw new IllegalArgumentException("Horário indisponível. Já existe outro agendamento nesse horário.");
        }

        // Atualiza o agendamento
        agendamento.setServico(servico);
        agendamento.setPrestador(prestador);
        agendamento.setDataHora(inicio);
        agendamento.setStatus(StatusAgendamento.PENDENTE);

        return agendamentoRepository.save(agendamento);
    }

    // Detecta a sobreposição de horarios
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

    @Scheduled(fixedRate = 300000) // a cada 5 minutos verifica se agendamento passou o horário
    @Transactional
    public void concluirAgendamentosVencidos() {
        List<Agendamento> pendentes = agendamentoRepository.findByStatus(StatusAgendamento.PENDENTE);
        LocalDateTime agora = LocalDateTime.now();

        // conclui agendamento pendente que passou o horario
        for (Agendamento ag : pendentes) {
            LocalDateTime fim = ag.getDataHora().plusMinutes(ag.getServico().getDuracaoMinutos());
            if (fim.isBefore(agora)) {
                ag.setStatus(StatusAgendamento.CONCLUIDO);
                System.out.println("Agendamento #" + ag.getId() +
                        " concluído automaticamente (término: " + fim + ")");

            }
        }
    }


}

