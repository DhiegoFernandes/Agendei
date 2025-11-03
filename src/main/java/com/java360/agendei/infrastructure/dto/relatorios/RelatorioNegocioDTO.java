package com.java360.agendei.infrastructure.dto.relatorios;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
public class RelatorioNegocioDTO {
    private String nomeNegocio;
    private YearMonth mes;
    private BigDecimal ganhosTotais;
    private long totalServicos;
    private List<PrestadorRelatorioDTO> prestadores;
}