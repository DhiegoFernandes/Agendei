package com.java360.agendei.infrastructure.controller;


import com.java360.agendei.domain.applicationservice.DisponibilidadeService;
import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.infrastructure.dto.DisponibilidadeDTO;
import com.java360.agendei.infrastructure.dto.SaveDisponibilidadeDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/disponibilidades")
@RequiredArgsConstructor
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidadeService;

    @PostMapping
    public ResponseEntity<DisponibilidadeDTO> cadastrar(@RequestBody @Valid SaveDisponibilidadeDTO dto) {
        Disponibilidade disponibilidade = disponibilidadeService.cadastrarOuAtualizarDisponibilidade(dto);
        return ResponseEntity
                .created(URI.create("/disponibilidades/" + disponibilidade.getId()))
                .body(DisponibilidadeDTO.fromEntity(disponibilidade));
    }

    @GetMapping("/prestador/{id}")
    public ResponseEntity<List<DisponibilidadeDTO>> listarPorPrestador(@PathVariable("id") String prestadorId) {
        var lista = disponibilidadeService
                .listarPorPrestador(prestadorId)
                .stream()
                .map(DisponibilidadeDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
