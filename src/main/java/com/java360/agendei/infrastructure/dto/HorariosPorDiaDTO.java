package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HorariosPorDiaDTO {
    private DiaSemanaDisponivel dia;
    private List<String> horarios; // formato "HH:mm"

}
