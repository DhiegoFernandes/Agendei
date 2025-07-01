package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import lombok.Data;

import java.time.LocalTime;

@Data
public class DisponibilidadeDTO {
    private final String id;
    private final DiaSemanaDisponivel diaSemana;
    private final LocalTime horaInicio;
    private final LocalTime horaFim;
    private final String prestadorId;
    private final String nomePrestador;

    public static DisponibilidadeDTO fromEntity(Disponibilidade d) {
        return new DisponibilidadeDTO(
                d.getId(),
                d.getDiaSemana(),
                d.getHoraInicio(),
                d.getHoraFim(),
                d.getPrestador().getId(),
                d.getPrestador().getNome()
        );
    }
}
