package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AgendamentoService;
import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.infrastructure.dto.AgendamentoDTO;
import com.java360.agendei.infrastructure.dto.CreateAgendamentoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<AgendamentoDTO> criar(@RequestBody @Valid CreateAgendamentoDTO dto) {
        dto.setIdAgendamento(null); // garante criação
        Agendamento agendamento = agendamentoService.salvarOuAtualizarAgendamento(dto);
        return ResponseEntity.ok(AgendamentoDTO.fromEntity(agendamento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> atualizar(@PathVariable Integer id, @RequestBody @Valid CreateAgendamentoDTO dto) {
        dto.setIdAgendamento(id);
        Agendamento agendamento = agendamentoService.salvarOuAtualizarAgendamento(dto);
        return ResponseEntity.ok(AgendamentoDTO.fromEntity(agendamento));
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Integer id) {
        agendamentoService.concluirAgendamento(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Integer id) {
        agendamentoService.cancelarAgendamento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cliente")
    public ResponseEntity<List<AgendamentoDTO>> listarCliente() {
        var lista = agendamentoService.listarAgendamentosCliente()
                .stream()
                .map(AgendamentoDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/prestador")
    public ResponseEntity<List<AgendamentoDTO>> listarPrestador() {
        var lista = agendamentoService.listarAgendamentosPrestador()
                .stream()
                .map(AgendamentoDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(lista);
    }

}

