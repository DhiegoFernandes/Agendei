package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.model.CategoriaNegocio;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NegocioResumoDTO {
    private Integer id;
    private String nome;
    private String endereco;
    private String cep;
    private CategoriaNegocio categoria;
    private boolean ativo;

    public static NegocioResumoDTO fromEntity(Negocio negocio) {
        return NegocioResumoDTO.builder()
                .id(negocio.getId())
                .nome(negocio.getNome())
                .endereco(negocio.getEndereco())
                .cep(negocio.getCep())
                .categoria(negocio.getCategoria())
                .ativo(negocio.isAtivo())
                .build();
    }
}
