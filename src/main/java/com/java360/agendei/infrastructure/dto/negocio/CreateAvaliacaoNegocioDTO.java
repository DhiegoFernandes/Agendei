package com.java360.agendei.infrastructure.dto.negocio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAvaliacaoNegocioDTO {

    private Integer agendamentoId;

    @NotNull(message = "A nota é obrigatória.")
    @Min(value = 1, message = "A nota mínima é 1.")
    @Max(value = 5, message = "A nota máxima é 5.")
    private int nota; // 0 a 5

    private String comentario;
}