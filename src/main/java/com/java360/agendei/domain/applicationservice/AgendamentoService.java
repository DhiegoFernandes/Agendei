package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.ClienteBloqueadoRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.ClienteResumoDTO;
import com.java360.agendei.infrastructure.dto.CreateAgendamentoDTO;
import com.java360.agendei.infrastructure.email.EmailService;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeService disponibilidadeService;
    private final ClienteBloqueadoRepository clienteBloqueadoRepository;
    private final EmailService emailService;

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

        if (dto.getServicoId() == null)
            throw new IllegalArgumentException("O ID do serviço é obrigatório ao criar um agendamento.");

        if (!dto.getDataHora().isAfter(LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))))
            throw new IllegalArgumentException("Não é possível criar um agendamento no passado.");

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!servico.isAtivo())
            throw new IllegalArgumentException("Não é possível agendar um serviço inativo.");

        Prestador prestador = servico.getPrestador();
        Negocio negocio = prestador.getNegocio();

        if (negocio == null || !negocio.isAtivo())
            throw new IllegalArgumentException("Não é possível agendar um serviço de um negócio inativo.");

        // BLOQUEIO POR NEGÓCIO
        boolean bloqueado = clienteBloqueadoRepository
                .findByNegocioIdAndClienteId(negocio.getId(), usuario.getId())
                .map(ClienteBloqueado::isAtivo)
                .orElse(false);

        if (bloqueado)
            throw new IllegalArgumentException("Você está bloqueado por este negócio e não pode agendar serviços.");

        int duracao = servico.getDuracaoMinutos();
        LocalDateTime inicio = dto.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(duracao);

        // impede agendamentos no horário de almoço
        LocalTime almocoInicio = prestador.getHoraInicioAlmoco();
        LocalTime almocoFim = prestador.getHoraFimAlmoco();

        if (almocoInicio != null && almocoFim != null) {
            LocalDate data = inicio.toLocalDate();
            LocalDateTime almocoInicioDT = LocalDateTime.of(data, almocoInicio);
            LocalDateTime almocoFimDT = LocalDateTime.of(data, almocoFim);

            if (overlaps(inicio, fim, almocoInicioDT, almocoFimDT))
                throw new IllegalArgumentException("Não é possível agendar durante o horário de almoço do prestador.");
        }

        if (!disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), inicio, duracao))
            throw new IllegalArgumentException("O prestador não está disponível nesse horário.");

        // Verifica sobreposição de horários com outros agendamentos pendentes
        List<Agendamento> agendamentosExistentes = agendamentoRepository.findByPrestadorId(prestador.getId());

        boolean conflita = agendamentosExistentes.stream()
                .filter(ag -> ag.getStatus() == StatusAgendamento.PENDENTE)
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

        // Salva o agendamento
        Agendamento salvo = agendamentoRepository.save(agendamento);

        // Envia email assincrono (não trava o fluxo)
        enviarEmailConfirmacaoAsync(salvo);

        //Retorna agendamento salvo
        return salvo;
    }

    @Async
    public void enviarEmailConfirmacaoAsync(Agendamento agendamento) {
        try {
            emailService.enviarConfirmacaoAgendamento(agendamento);
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail: " + e.getMessage());
        }
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

        if (!dto.getDataHora().isAfter(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))) {
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

    @Transactional
    public List<ClienteResumoDTO> listarClientesDoPrestador() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR);

        Prestador prestador = (Prestador) usuario;
        Negocio negocio = prestador.getNegocio();

        if (negocio == null) {
            throw new IllegalArgumentException("Prestador não pertence a um negócio.");
        }

        // Agendamentos de TODOS os prestadores do mesmo negócio
        List<Agendamento> agendamentos = agendamentoRepository
                .findByPrestador_Negocio_Id(negocio.getId());

        return agendamentos.stream()
                .map(Agendamento::getCliente)
                .distinct()
                .map(c -> {

                    long total = agendamentos.stream()
                            .filter(a -> a.getCliente().getId().equals(c.getId()))
                            .count();

                    long cancelados = agendamentos.stream()
                            .filter(a -> a.getCliente().getId().equals(c.getId()))
                            .filter(a -> a.getStatus() == StatusAgendamento.CANCELADO)
                            .count();

                    double taxa = (total > 0) ? ((double) cancelados / total) * 100 : 0.0;

                    boolean bloqueado = clienteBloqueadoRepository
                            .findByNegocioIdAndClienteId(negocio.getId(), c.getId())
                            .map(ClienteBloqueado::isAtivo)
                            .orElse(false);

                    return new ClienteResumoDTO(
                            c.getId(),
                            c.getNome(),
                            c.getEmail(),
                            c.getTelefone(),
                            bloqueado,
                            Math.round(taxa * 100.0) / 100.0
                    );
                })
                .toList();
    }




    @Transactional
    public List<ClienteResumoDTO> listarClientesBloqueados() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR);

        Prestador prestador = (Prestador) usuario;
        Negocio negocio = prestador.getNegocio();

        if (negocio == null) {
            throw new IllegalArgumentException("Prestador não pertence a um negócio.");
        }

        List<ClienteBloqueado> bloqueios = clienteBloqueadoRepository
                .findByNegocioId(negocio.getId())
                .stream()
                .filter(ClienteBloqueado::isAtivo)
                .toList();

        // Busca agendamentos do negócio inteiro
        List<Agendamento> agendamentos = agendamentoRepository
                .findByPrestador_Negocio_Id(negocio.getId());

        return bloqueios.stream()
                .map(b -> {
                    Cliente c = b.getCliente();

                    long total = agendamentos.stream()
                            .filter(a -> a.getCliente().getId().equals(c.getId()))
                            .count();

                    long cancelados = agendamentos.stream()
                            .filter(a -> a.getCliente().getId().equals(c.getId()))
                            .filter(a -> a.getStatus() == StatusAgendamento.CANCELADO)
                            .count();

                    double taxa = (total > 0) ? ((double) cancelados / total) * 100 : 0.0;

                    return new ClienteResumoDTO(
                            c.getId(),
                            c.getNome(),
                            c.getEmail(),
                            c.getTelefone(),
                            true, // bloqueado
                            Math.round(taxa * 100.0) / 100.0
                    );
                })
                .toList();
    }



    @Transactional
    public void bloquearCliente(Integer clienteId) {
        Usuario atual = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(atual, PerfilUsuario.PRESTADOR);

        Prestador prestador = (Prestador) atual;
        Negocio negocio = prestador.getNegocio();

        if (negocio == null)
            throw new IllegalArgumentException("Prestador não pertence a um negócio.");

        Usuario u = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!(u instanceof Cliente cliente))
            throw new IllegalArgumentException("Apenas clientes podem ser bloqueados.");

        ClienteBloqueado bloqueio = clienteBloqueadoRepository
                .findByNegocioIdAndClienteId(negocio.getId(), cliente.getId())
                .orElse(ClienteBloqueado.builder()
                        .negocio(negocio)
                        .cliente(cliente)
                        .build());

        bloqueio.setAtivo(true);
        clienteBloqueadoRepository.save(bloqueio);

        // Cancela todos os agendamentos pendentes do cliente com este prestador
        agendamentoRepository.findByPrestadorId(prestador.getId()).stream()
                .filter(a -> a.getCliente().getId().equals(clienteId))
                .filter(a -> a.getStatus() == StatusAgendamento.PENDENTE)
                .forEach(a -> a.setStatus(StatusAgendamento.CANCELADO));
    }




    @Transactional
    public void desbloquearCliente(Integer clienteId) {
        Usuario atual = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(atual, PerfilUsuario.PRESTADOR);

        Prestador prestador = (Prestador) atual;
        Negocio negocio = prestador.getNegocio();

        ClienteBloqueado bloqueio = clienteBloqueadoRepository
                .findByNegocioIdAndClienteId(negocio.getId(), clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não está bloqueado."));

        bloqueio.setAtivo(false);
        clienteBloqueadoRepository.save(bloqueio);
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

