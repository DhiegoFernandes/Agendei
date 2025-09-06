package com.java360.agendei.infrastructure.dto.relatorios;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@AllArgsConstructor
public class EvolucaoMensalDTO {
    private YearMonth mes;
    private BigDecimal faturamento;
}
