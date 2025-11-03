package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.RelatorioService;
import com.java360.agendei.infrastructure.dto.relatorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


    // ########## Relatorios do prestador dono do negocio ########

    @GetMapping("/negocio/{id}")
    public ResponseEntity<RelatorioNegocioDTO> relatorioNegocio(
            @PathVariable Integer id,
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        YearMonth yearMonth = YearMonth.of(ano, mes);
        RelatorioNegocioDTO relatorio = relatorioService.relatorioNegocio(id, yearMonth);
        return ResponseEntity.ok(relatorio);
    }


}
