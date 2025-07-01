package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.model.AgendamentoStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AgendamentoDTO {
    private final String id;
    private final LocalDateTime dataHora;
    private final AgendamentoStatus status;
    private final String clienteId;
    private final String servicoId;
    private final String tituloServico;

    public static AgendamentoDTO fromEntity(Agendamento ag) {
        return new AgendamentoDTO(
                ag.getId(),
                ag.getDataHora(),
                ag.getStatus(),
                ag.getCliente().getId(),
                ag.getServico().getId(),
                ag.getServico().getTitulo()
        );
    }
}
