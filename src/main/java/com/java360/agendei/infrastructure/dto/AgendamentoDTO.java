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
    private String nomeCliente;
    private String nomePrestador;
    private String tituloServico;
    private LocalDateTime dataHora;
    private StatusAgendamento status;

    public static AgendamentoDTO fromEntity(Agendamento agendamento) {
        return AgendamentoDTO.builder()
                .id(agendamento.getId())
                .nomeCliente(agendamento.getCliente().getNome())
                .nomePrestador(agendamento.getPrestador().getNome())
                .tituloServico(agendamento.getServico().getTitulo())
                .dataHora(agendamento.getDataHora())
                .status(agendamento.getStatus())
                .build();
    }
}

