package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.RelatorioService;
import com.java360.agendei.infrastructure.dto.relatorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/financeiro-mensal")
    public ResponseEntity<RelatorioFinanceiroDTO> financeiroMensal(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(relatorioService.relatorioFinanceiroMensal(mes));
    }

    @GetMapping("/evolucao-mensal")
    public ResponseEntity<List<EvolucaoMensalDTO>> evolucaoMensal(
            @RequestParam int ano) {
        return ResponseEntity.ok(relatorioService.evolucaoMensal(ano));
    }

    @GetMapping("/evolucao-anual")
    public ResponseEntity<List<EvolucaoAnualDTO>> evolucaoAnual(
            @RequestParam int anoInicio,
            @RequestParam int anoFim) {
        return ResponseEntity.ok(relatorioService.evolucaoAnual(anoInicio, anoFim));
    }

    @GetMapping("/servicos-mais-vendidos")
    public ResponseEntity<List<ServicoMaisVendidoDTO>> servicosMaisVendidos(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(relatorioService.servicosMaisVendidos(mes));
    }

}
