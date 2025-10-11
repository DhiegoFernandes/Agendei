package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.CategoriaNegocio;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // ignora campos nulos no JSON
public class UpdateNegocioDTO {

    private String nome;
    private String endereco;
    private String cep;
    private CategoriaNegocio categoria;
    private Boolean ativo; // somente admin pode alterar
}
