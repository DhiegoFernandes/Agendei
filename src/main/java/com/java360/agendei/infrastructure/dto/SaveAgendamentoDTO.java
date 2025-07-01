package com.java360.agendei.infrastructure.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SaveAgendamentoDTO {

    @NotBlank
    private String clienteId;

    @NotBlank
    private String servicoId;

    @NotNull
    @Future(message = "A data/hora deve ser no futuro")
    private LocalDateTime dataHora;
}


