package com.java360.agendei.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FotoPrestadorDTO {
    private Integer prestadorId;
    private String nomePrestador;
    private String urlFoto; // endpoint para baixar
}
