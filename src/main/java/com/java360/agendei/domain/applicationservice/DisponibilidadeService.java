package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.DisponibilidadeRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.DisponibilidadeDTO;
import com.java360.agendei.infrastructure.dto.HorarioAlmocoDTO;
import com.java360.agendei.infrastructure.dto.SaveDisponibilidadeDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;

    public boolean prestadorEstaDisponivel(Integer prestadorId, LocalDateTime inicioAgendamento, int duracaoMinutos) {
        DayOfWeek diaSemana = inicioAgendamento.getDayOfWeek();
        LocalTime inicio = inicioAgendamento.toLocalTime();
        LocalTime fim = inicio.plusMinutes(duracaoMinutos);

        System.out.println("Validando disponibilidade para dia " + diaSemana + " - Início: " + inicio + " Fim: " + fim);
        disponibilidadeRepository.findByPrestadorId(prestadorId).forEach(d ->
                System.out.println("Disponibilidade: " + d.getDiaSemana() + " de " + d.getHoraInicio() + " até " + d.getHoraFim())
        );


        return disponibilidadeRepository.findByPrestadorId(prestadorId).stream()
                .filter(Disponibilidade::isAtivo) // só considera dias ativos
                .anyMatch(d ->
                        d.getDiaSemana().equals(DiaSemanaDisponivel.valueOf(traduzirDiaDaSemana(diaSemana))) &&
                                !inicio.isBefore(d.getHoraInicio()) &&
                                !fim.isAfter(d.getHoraFim())
                );

    }

    private String traduzirDiaDaSemana(DayOfWeek dia) {
        return switch (dia) {
            case SUNDAY -> "DOMINGO";
            case MONDAY -> "SEGUNDA";
            case TUESDAY -> "TERCA";
            case WEDNESDAY -> "QUARTA";
            case THURSDAY -> "QUINTA";
            case FRIDAY -> "SEXTA";
            case SATURDAY -> "SABADO";
        };
    }

    @Transactional
    public DisponibilidadeDTO cadastrarOuAtualizarDisponibilidade(SaveDisponibilidadeDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;

        if (dto.getHoraInicio().isAfter(dto.getHoraFim()) || dto.getHoraInicio().equals(dto.getHoraFim())) {
            throw new IllegalArgumentException("Horário de início deve ser antes do horário de fim.");
        }
        if (dto.getHoraFim().isAfter(LocalTime.of(23, 59))) {
            throw new IllegalArgumentException("Horário de fim deve ser até 23:59.");
        }

        Disponibilidade disponibilidade = disponibilidadeRepository
                .findByPrestadorIdAndDiaSemana(prestador.getId(), dto.getDiaSemana())
                .map(d -> {
                    d.setHoraInicio(dto.getHoraInicio());
                    d.setHoraFim(dto.getHoraFim());
                    return d;
                })
                .orElseGet(() -> Disponibilidade.builder()
                        .prestador(prestador)
                        .diaSemana(dto.getDiaSemana())
                        .horaInicio(dto.getHoraInicio())
                        .horaFim(dto.getHoraFim())
                        .build()
                );

        Disponibilidade salvo = disponibilidadeRepository.save(disponibilidade);

        return DisponibilidadeDTO.fromEntity(salvo);
    }

    @Transactional
    public Disponibilidade alterarStatusDia(DiaSemanaDisponivel dia, boolean ativo) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;

        Disponibilidade disponibilidade = disponibilidadeRepository
                .findByPrestadorIdAndDiaSemana(prestador.getId(), dia)
                .orElseThrow(() -> new IllegalArgumentException("Dia não cadastrado para este prestador."));

        disponibilidade.setAtivo(ativo);
        return disponibilidade;
    }

    @Transactional
    public void definirHorarioAlmoco(LocalTime horaInicio) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR);

        Prestador prestador = (Prestador) usuario;

        if (horaInicio.isBefore(LocalTime.of(5, 0)) || horaInicio.isAfter(LocalTime.of(20, 0))) {
            throw new IllegalArgumentException("Horário de almoço deve estar entre 05:00 e 20:00.");
        }

        LocalTime novoInicio = horaInicio;
        LocalTime novoFim = horaInicio.plusHours(1);

        // Salva novo horário de almoço
        prestador.setHoraInicioAlmoco(novoInicio);
        prestador.setHoraFimAlmoco(novoFim);
        usuarioRepository.save(prestador);

        // CANCELA AGENDAMENTOS DENTRO DO NOVO HORÁRIO DE ALMOÇO
        cancelarAgendamentosDuranteAlmoco(prestador, novoInicio, novoFim);
    }

    @Transactional
    private void cancelarAgendamentosDuranteAlmoco(Prestador prestador, LocalTime inicio, LocalTime fim) {
        List<Agendamento> agendamentos = agendamentoRepository.findByPrestadorId(prestador.getId());

        for (Agendamento ag : agendamentos) {

            if (ag.getStatus() != StatusAgendamento.PENDENTE) continue;

            LocalDateTime agInicio = ag.getDataHora();
            LocalDateTime agFim = ag.getDataHora().plusMinutes(ag.getServico().getDuracaoMinutos());

            LocalDateTime almocoInicioDT = LocalDateTime.of(agInicio.toLocalDate(), inicio);
            LocalDateTime almocoFimDT = LocalDateTime.of(agInicio.toLocalDate(), fim);

            if (overlaps(agInicio, agFim, almocoInicioDT, almocoFimDT)) {
                ag.setStatus(StatusAgendamento.CANCELADO);
                agendamentoRepository.save(ag);

            }
        }
    }

    private boolean overlaps(LocalDateTime inicio1, LocalDateTime fim1,
                             LocalDateTime inicio2, LocalDateTime fim2) {
        return inicio1.isBefore(fim2) && fim1.isAfter(inicio2);
    }



    @Transactional
    public HorarioAlmocoDTO buscarHorarioAlmoco() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR);

        Prestador prestador = (Prestador) usuario;

        LocalTime inicio = prestador.getHoraInicioAlmoco();
        LocalTime fim = prestador.getHoraFimAlmoco();

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Horário de almoço ainda não foi definido.");
        }

        return new HorarioAlmocoDTO(inicio, fim);
    }


    public List<Disponibilidade> listarPorPrestadorAutenticado() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;
        return disponibilidadeRepository.findByPrestadorId(prestador.getId());
    }

}