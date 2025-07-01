package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class SaveDisponibilidadeDTO {

    @NotNull
    private DiaSemanaDisponivel diaSemana;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFim;

    @NotNull
    private String prestadorId;
}
