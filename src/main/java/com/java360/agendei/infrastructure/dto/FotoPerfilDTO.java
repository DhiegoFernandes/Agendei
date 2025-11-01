package com.java360.agendei.infrastructure.dto;

import lombok.Data;

@Data
public class FotoPerfilDTO {
    private String base64Imagem; // Enviar imagem codificada em Base64
    private String nomeArquivo;
}
