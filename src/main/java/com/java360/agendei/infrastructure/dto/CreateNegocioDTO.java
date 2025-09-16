package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.CategoriaNegocio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateNegocioDTO {

    @NotBlank
    private String nome;

    @NotBlank
    private String endereco;

    @NotBlank
    private String cep;

    @NotNull
    private CategoriaNegocio categoria;

}
