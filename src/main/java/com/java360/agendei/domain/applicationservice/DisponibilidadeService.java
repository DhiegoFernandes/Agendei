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
import java.util.Optional;

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
    public Disponibilidade cadastrarOuAtualizarDisponibilidade(SaveDisponibilidadeDTO dto) {
        Prestador prestador = (Prestador) usuarioRepository.findById(dto.getPrestadorId())
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        if (dto.getHoraInicio().isAfter(dto.getHoraFim()) || dto.getHoraInicio().equals(dto.getHoraFim())) {
            throw new IllegalArgumentException("Horário de início deve ser antes do horário de fim.");
        }

        Optional<Disponibilidade> existente = disponibilidadeRepository
                .findByPrestadorIdAndDiaSemana(dto.getPrestadorId(), dto.getDiaSemana());

        if (existente.isPresent()) {
            // Atualiza a disponibilidade existente
            Disponibilidade d = existente.get();
            d.setHoraInicio(dto.getHoraInicio());
            d.setHoraFim(dto.getHoraFim());
            return d;
        }

        // Nova disponibilidade
        Disponibilidade nova = Disponibilidade.builder()
                .prestador(prestador)
                .diaSemana(dto.getDiaSemana())
                .horaInicio(dto.getHoraInicio())
                .horaFim(dto.getHoraFim())
                .build();

        return disponibilidadeRepository.save(nova);
    }

    public List<Disponibilidade> listarPorPrestador(String prestadorId) {
        return disponibilidadeRepository.findByPrestadorId(prestadorId);
    }

}