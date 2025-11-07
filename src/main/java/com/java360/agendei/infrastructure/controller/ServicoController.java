package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.ServicoService;
import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import com.java360.agendei.infrastructure.dto.HorariosDisponiveisDTO;
import com.java360.agendei.infrastructure.dto.SaveServicoDTO;
import com.java360.agendei.infrastructure.dto.ServicoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    //Cadastra serviço
    @PostMapping
    public ResponseEntity<ServicoDTO> cadastrar(@RequestBody @Valid SaveServicoDTO dto) {
        Servico servico = servicoService.cadastrarServico(dto);
        return ResponseEntity
                .created(URI.create("/servicos" + "/" + servico.getId()))
                .body(ServicoDTO.fromEntity(servico));
    }

    //Verifica horarios disponiveis em uma data especifica (?data=2025-12-30)
    @GetMapping("/{id}/horarios-disponiveis-data")
    public ResponseEntity<HorariosDisponiveisDTO> listarHorariosPorData(
            @PathVariable("id") Integer servicoId,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        var dto = servicoService.listarHorariosPorServicoEData(servicoId, data);
        return ResponseEntity.ok(dto);
    }

    // Atualiza serviço
    @PutMapping("/{id}")
    public ResponseEntity<ServicoDTO> atualizar(@PathVariable Integer id, @RequestBody @Valid SaveServicoDTO dto) {
        Servico servico = servicoService.atualizarServico(id, dto);
        return ResponseEntity.ok(ServicoDTO.fromEntity(servico));
    }

    // Busca serviço
    @GetMapping("/busca")
    public ResponseEntity<List<ServicoDTO>> buscar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String nomePrestador,
            @RequestParam(required = false) DiaSemanaDisponivel diaSemana
    ) {
        var resultado = servicoService.buscarServicos(titulo, nomePrestador, diaSemana);
        return ResponseEntity.ok(resultado);
    }

    // Lista TODOS serviços ativos
    @GetMapping("/ativos")
    public ResponseEntity<List<ServicoDTO>> listarAtivos() {
        List<ServicoDTO> lista = servicoService
                .listarServicosAtivos()
                .stream()
                .map(ServicoDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(lista);
    }

    // Lista todos serviços ATIVOS por negocio
    @GetMapping("/negocio/{id}")
    public ResponseEntity<List<ServicoDTO>> listarPorNegocio(@PathVariable Integer id) {
        List<ServicoDTO> lista = servicoService.listarServicosPorNegocio(id);
        return ResponseEntity.ok(lista);
    }

    // Lista todos os serviços por negócio (ativos/inativos)
    @GetMapping("/negocio/{id}/todos")
    public ResponseEntity<List<ServicoDTO>> listarTodosPorNegocio(@PathVariable Integer id) {
        List<ServicoDTO> lista = servicoService.listarTodosServicosPorNegocio(id);
        return ResponseEntity.ok(lista);
    }



}
