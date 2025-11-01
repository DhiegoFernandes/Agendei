package com.java360.agendei.infrastructure.dto.negocio;

import com.java360.agendei.domain.model.CategoriaNegocio;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // ignora campos nulos no JSON
public class UpdateNegocioDTO {

    private String nome;

    @Size(max = 200, message = "Endereço deve ter no máximo 200 caracteres.")
    private String endereco;

    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres.")
    private String numero;

    @Size(max = 9, message = "CEP deve ter no máximo 9 caracteres.")
    private String cep;

    private CategoriaNegocio categoria;
    private Boolean ativo; // somente admin pode alterar
}
