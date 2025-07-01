package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.model.AgendamentoStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AgendamentoDTO {
    private final String id;
    private final String name;
    private final String description;
    private final LocalDate initialDate;
    private final LocalDate finalDate;
    private final AgendamentoStatus status;

    public static AgendamentoDTO create(Agendamento agendamento) {
        return new AgendamentoDTO(
                agendamento.getId(),
                agendamento.getName(),
                agendamento.getDescription(),
                agendamento.getInitialDate(),
                agendamento.getFinalDate(),
                agendamento.getStatus()
        );
    }
}
