package com.java360.agendei.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class HorarioAlmocoDTO {
    private LocalTime horaInicioAlmoco;
    private LocalTime horaFimAlmoco;
}
