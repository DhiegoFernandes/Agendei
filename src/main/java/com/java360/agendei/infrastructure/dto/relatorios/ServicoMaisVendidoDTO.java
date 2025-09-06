package com.java360.agendei.infrastructure.dto.relatorios;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ServicoMaisVendidoDTO {
    private String tituloServico;
    private long quantidadeAgendamentos;
    private BigDecimal totalFaturado;
}
