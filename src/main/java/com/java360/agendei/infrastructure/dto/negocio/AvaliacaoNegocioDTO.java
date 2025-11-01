package com.java360.agendei.infrastructure.dto.negocio;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AvaliacaoNegocioDTO {
    private Integer id;
    private Integer negocioId;
    private String nomeNegocio;
    private Integer clienteId;
    private String nomeCliente;
    private int nota;
    private String comentario;
    private LocalDateTime dataAvaliacao;
}
