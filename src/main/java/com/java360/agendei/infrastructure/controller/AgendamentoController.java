package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AgendamentoService;
import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.infrastructure.dto.AgendamentoDTO;
import com.java360.agendei.infrastructure.dto.ClienteResumoDTO;
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
        Agendamento agendamento = agendamentoService.criarAgendamento(dto);
        return ResponseEntity.ok(AgendamentoDTO.fromEntity(agendamento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid CreateAgendamentoDTO dto
    ) {
        Agendamento agendamento = agendamentoService.atualizarAgendamento(id, dto);
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

    // Lista todos os clientes do prestador
    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteResumoDTO>> listarClientesDoPrestador() {
        return ResponseEntity.ok(agendamentoService.listarClientesDoPrestador());
    }

    // Lista todos os clientes bloqueados do prestador
    @GetMapping("/clientes/bloqueados")
    public ResponseEntity<List<ClienteResumoDTO>> listarClientesBloqueados() {
        return ResponseEntity.ok(agendamentoService.listarClientesBloqueados());
    }

    //Bloqueia cliente
    @PutMapping("/clientes/{clienteId}/bloquear")
    public ResponseEntity<String> bloquearCliente(@PathVariable Integer clienteId) {
        agendamentoService.bloquearCliente(clienteId);
        return ResponseEntity.ok("Cliente bloqueado com sucesso.");
    }

    //Desbloqueia cliente
    @PutMapping("/clientes/{clienteId}/desbloquear")
    public ResponseEntity<String> desbloquearCliente(@PathVariable Integer clienteId) {
        agendamentoService.desbloquearCliente(clienteId);
        return ResponseEntity.ok("Cliente desbloqueado com sucesso.");
    }

}

