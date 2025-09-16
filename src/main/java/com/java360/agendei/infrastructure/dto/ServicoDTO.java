package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.model.CategoriaServico;
import lombok.Data;

@Data
public class ServicoDTO {
    private final Integer id;
    private final String titulo;
    private final String descricao;
    private final double valor;
    private final int duracaoMinutos;
    private final boolean ativo;
    private final Integer prestadorId;
    private final String nomePrestador;
    private final Integer negocioId;

    public static ServicoDTO fromEntity(Servico servico) {
        return new ServicoDTO(
                servico.getId(),
                servico.getTitulo(),
                servico.getDescricao(),
                servico.getValor(),
                servico.getDuracaoMinutos(),
                servico.isAtivo(),
                servico.getPrestador().getId(),
                servico.getPrestador().getNome(),
                servico.getNegocio().getId()
        );
    }
}
