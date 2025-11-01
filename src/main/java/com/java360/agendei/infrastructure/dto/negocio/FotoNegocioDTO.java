package com.java360.agendei.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FotoNegocioDTO {
    private Integer id;
    private String nomeArquivo;
    private String url; // Endpoint para baixar a imagem
}
