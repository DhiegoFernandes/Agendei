package com.java360.agendei.infrastructure.dto.negocio;

import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.model.CategoriaNegocio;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NegocioBuscaDTO {
    private Integer id;
    private String nome;
    private String endereco;
    private String cep;
    private Double notaMedia;
    private Double distanciaKm;
    private CategoriaNegocio categoria;

    public static NegocioBuscaDTO fromEntity(Negocio negocio, double distanciaKm) {
        return NegocioBuscaDTO.builder()
                .id(negocio.getId())
                .nome(negocio.getNome())
                .endereco(negocio.getEndereco())
                .cep(negocio.getCep())
                .notaMedia(negocio.getNotaMedia())
                .distanciaKm(distanciaKm)
                .categoria(negocio.getCategoria())
                .build();
    }
}
