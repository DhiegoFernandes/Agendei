package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.DisponibilidadeRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.DisponibilidadeDTO;
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


    public List<Disponibilidade> listarPorPrestadorAutenticado() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;
        return disponibilidadeRepository.findByPrestadorId(prestador.getId());
    }

}