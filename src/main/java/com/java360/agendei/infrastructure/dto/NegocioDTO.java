package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Negocio;
import lombok.Data;

@Data
public class NegocioDTO {
    private final Integer id;
    private final String nome;
    private final String endereco;
    private final String cep;

    public static NegocioDTO fromEntity(Negocio negocio) {
        return new NegocioDTO(
                negocio.getId(),
                negocio.getNome(),
                negocio.getEndereco(),
                negocio.getCep()
        );
    }
}