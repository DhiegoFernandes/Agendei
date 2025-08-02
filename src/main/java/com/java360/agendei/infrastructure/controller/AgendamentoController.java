package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AgendamentoService;
import com.java360.agendei.infrastructure.dto.AgendamentoDTO;
import com.java360.agendei.infrastructure.dto.CreateAgendamentoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<AgendamentoDTO> criar(@RequestBody @Valid CreateAgendamentoDTO dto) {
        var agendamento = agendamentoService.criarAgendamento(dto);
        return ResponseEntity.ok(AgendamentoDTO.fromEntity(agendamento));
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Integer id) {
        agendamentoService.concluirAgendamento(id);
        return ResponseEntity.ok().build();
    }
}

