package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.model.StatusAgendamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class AgendamentoDTO {

    private Integer id;
    private String clienteNome;
    private String prestadorNome;
    private String servicoTitulo;
    private LocalDateTime dataHora;
    private StatusAgendamento status;

    public static AgendamentoDTO fromEntity(Agendamento agendamento) {
        return new AgendamentoDTO(
                agendamento.getId(),
                agendamento.getCliente().getNome(),
                agendamento.getPrestador().getNome(),
                agendamento.getServico().getTitulo(),
                agendamento.getDataHora(),
                agendamento.getStatus()
        );
    }
}

