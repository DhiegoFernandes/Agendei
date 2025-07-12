package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.model.CategoriaServico;
import lombok.Data;

@Data
public class ServicoDTO {
    private final String id;
    private final String titulo;
    private final String descricao;
    private final CategoriaServico categoria;
    private final double valor;
    private final int duracaoMinutos;
    private final boolean ativo;
    private final String prestadorId;
    private final String nomePrestador;
    private final String negocioId;

    public static ServicoDTO fromEntity(Servico servico) {
        return new ServicoDTO(
                servico.getId(),
                servico.getTitulo(),
                servico.getDescricao(),
                servico.getCategoria(),
                servico.getValor(),
                servico.getDuracaoMinutos(),
                servico.isAtivo(),
                servico.getPrestador().getId(),
                servico.getPrestador().getNome(),
                servico.getNegocio().getId()
        );
    }
}
