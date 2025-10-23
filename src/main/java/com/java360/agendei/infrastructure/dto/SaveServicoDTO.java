package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.CategoriaServico;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SaveServicoDTO {

    @NotBlank
    @Size(max = 100)
    private String titulo;

    @NotBlank
    @Size(max = 255)
    private String descricao;

    //@NotNull
    //private CategoriaServico categoria;

    @Positive
    private double valor;

    @Min(5)
    private int duracaoMinutos;

    @NotNull
    private Boolean ativo;

    @AssertTrue(message = "A duração deve ser positiva")
    public boolean isDuracaoValida() {
        return duracaoMinutos > 0;
    }
}