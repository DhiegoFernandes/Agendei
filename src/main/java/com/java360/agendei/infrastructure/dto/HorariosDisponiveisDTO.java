package com.java360.agendei.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HorariosDisponiveisDTO {
    private String servicoId;
    private List<HorariosPorDiaDTO> diasDisponiveis;

}
