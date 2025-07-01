package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.repository.DisponibilidadeRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.SaveDisponibilidadeDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final UsuarioRepository usuarioRepository;

    public boolean prestadorEstaDisponivel(String prestadorId, LocalDateTime dataHora) {
        DayOfWeek diaSemana = dataHora.getDayOfWeek();
        var disponiveis = disponibilidadeRepository.findByPrestadorId(prestadorId);

        return disponiveis.stream().anyMatch(d ->
                d.getDiaSemana().name().equalsIgnoreCase(diaSemana.name()) &&
                        !dataHora.toLocalTime().isBefore(d.getHoraInicio()) &&
                        !dataHora.toLocalTime().isAfter(d.getHoraFim())
        );
    }

    @Transactional
    public Disponibilidade cadastrarDisponibilidade(SaveDisponibilidadeDTO dto) {
        Prestador prestador = (Prestador) usuarioRepository.findById(dto.getPrestadorId())
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        if (dto.getHoraInicio().isAfter(dto.getHoraFim()) || dto.getHoraInicio().equals(dto.getHoraFim())) {
            throw new IllegalArgumentException("Horário de início deve ser antes do horário de fim.");
        }

        Disponibilidade disponibilidade = Disponibilidade.builder()
                .diaSemana(dto.getDiaSemana())
                .horaInicio(dto.getHoraInicio())
                .horaFim(dto.getHoraFim())
                .prestador(prestador)
                .build();

        return disponibilidadeRepository.save(disponibilidade);
    }

    public List<Disponibilidade> listarPorPrestador(String prestadorId) {
        return disponibilidadeRepository.findByPrestadorId(prestadorId);
    }

}