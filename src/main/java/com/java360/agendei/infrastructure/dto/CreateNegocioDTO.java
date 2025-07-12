package com.java360.agendei.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateNegocioDTO {

    @NotBlank
    private String nome;

    @NotBlank
    private String endereco;

    @NotBlank
    private String prestadorId;
}
