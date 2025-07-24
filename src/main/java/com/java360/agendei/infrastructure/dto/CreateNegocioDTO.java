package com.java360.agendei.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateNegocioDTO {

    @NotBlank
    private String nome;

    @NotBlank
    private String endereco;

    @NotNull
    private Integer prestadorId;
}
