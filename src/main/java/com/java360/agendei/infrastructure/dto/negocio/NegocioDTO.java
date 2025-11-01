package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.model.CategoriaNegocio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class NegocioDTO {
    private Integer id;
    private String nome;
    private String endereco;
    private String numero;
    private String cep;
    private CategoriaNegocio categoria;
    private boolean ativo;
    private Double notaMedia;

    public static NegocioDTO fromEntity(Negocio negocio) {
        return NegocioDTO.builder()
                .id(negocio.getId())
                .nome(negocio.getNome())
                .endereco(negocio.getEndereco())
                .numero(negocio.getNumero())
                .cep(negocio.getCep())
                .categoria(negocio.getCategoria())
                .ativo(negocio.isAtivo())
                .notaMedia(negocio.getNotaMedia())
                .build();
    }
}