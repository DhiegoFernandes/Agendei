package com.java360.agendei.infrastructure.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAgendamentoDTO {
    private Integer idAgendamento; // null = criar, != null = atualizar

    @NotNull
    private Integer servicoId;

    @NotNull
    private LocalDateTime dataHora;
}
