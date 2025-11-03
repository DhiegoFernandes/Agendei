package com.java360.agendei.infrastructure.dto.relatorios;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PrestadorRelatorioDTO {
    private Integer id;
    private String nome;
    private BigDecimal ganhos;
    private Double taxaCancelamento;
}