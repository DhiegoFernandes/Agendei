package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.model.CategoriaNegocio;
import lombok.Data;

@Data
public class NegocioDTO {
    private final Integer id;
    private final String nome;
    private final String endereco;
    private final String cep;
    private final CategoriaNegocio Categoria;

    public static NegocioDTO fromEntity(Negocio negocio) {
        return new NegocioDTO(
                negocio.getId(),
                negocio.getNome(),
                negocio.getEndereco(),
                negocio.getCep(),
                negocio.getCategoria()
        );
    }
}