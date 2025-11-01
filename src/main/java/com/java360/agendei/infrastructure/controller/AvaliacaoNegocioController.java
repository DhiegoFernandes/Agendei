package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AvaliacaoNegocioService;
import com.java360.agendei.infrastructure.dto.negocio.AvaliacaoNegocioDTO;
import com.java360.agendei.infrastructure.dto.negocio.CreateAvaliacaoNegocioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoNegocioController {

    private final AvaliacaoNegocioService avaliacaoService;

    @PostMapping
    public AvaliacaoNegocioDTO criar(@RequestBody CreateAvaliacaoNegocioDTO dto) {
        return avaliacaoService.criarAvaliacao(dto);
    }

    @GetMapping("/negocio/{negocioId}")
    public List<AvaliacaoNegocioDTO> listarPorNegocio(@PathVariable Integer negocioId) {
        return avaliacaoService.listarAvaliacoesNegocio(negocioId);
    }
}