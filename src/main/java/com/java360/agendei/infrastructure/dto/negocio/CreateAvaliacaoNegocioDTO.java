package com.java360.agendei.infrastructure.dto.negocio;

import lombok.Data;

@Data
public class CreateAvaliacaoNegocioDTO {
    private Integer agendamentoId;
    private int nota; // 0 a 5
    private String comentario;
}