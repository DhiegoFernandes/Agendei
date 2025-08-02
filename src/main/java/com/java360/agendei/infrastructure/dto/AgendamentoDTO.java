package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.model.StatusAgendamento;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AgendamentoDTO {
    private Integer id;
    private String nomeCliente;
    private String nomeServico;
    private LocalDateTime dataHora;
    private StatusAgendamento status;

    public static AgendamentoDTO fromEntity(Agendamento agendamento) {
        return new AgendamentoDTO(
                agendamento.getId(),
                agendamento.getCliente().getNome(),
                agendamento.getServico().getTitulo(),
                agendamento.getDataHora(),
                agendamento.getStatus()
        );
    }
}

