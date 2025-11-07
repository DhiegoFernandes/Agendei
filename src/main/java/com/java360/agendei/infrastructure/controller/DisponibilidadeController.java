package com.java360.agendei.infrastructure.controller;


import com.java360.agendei.domain.applicationservice.DisponibilidadeService;
import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import com.java360.agendei.infrastructure.dto.DisponibilidadeDTO;
import com.java360.agendei.infrastructure.dto.HorarioAlmocoDTO;
import com.java360.agendei.infrastructure.dto.SaveDisponibilidadeDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/disponibilidades")
@RequiredArgsConstructor
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidadeService;

    @PostMapping
    public ResponseEntity<DisponibilidadeDTO> cadastrarOuAtualizar(@RequestBody SaveDisponibilidadeDTO dto) {
        DisponibilidadeDTO resposta = disponibilidadeService.cadastrarOuAtualizarDisponibilidade(dto);
        return ResponseEntity.ok(resposta);
    }


    @PatchMapping("/status-dia")
    public ResponseEntity<DisponibilidadeDTO> alterarStatusDia(@RequestParam DiaSemanaDisponivel dia, @RequestParam boolean ativo) {
         DisponibilidadeDTO status = DisponibilidadeDTO.fromEntity(disponibilidadeService.alterarStatusDia(dia, ativo));
         return ResponseEntity.ok(status);
    }

    @PatchMapping("/almoco")
    public ResponseEntity<String> definirHorarioAlmoco(@RequestParam String horaInicio) {
        LocalTime inicio = LocalTime.parse(horaInicio);
        disponibilidadeService.definirHorarioAlmoco(inicio);
        return ResponseEntity.ok("Horário de almoço definido com sucesso: " + inicio + " às " + inicio.plusHours(1));
    }

    @GetMapping("/almoco")
    public ResponseEntity<HorarioAlmocoDTO> buscarHorarioAlmoco() {
        HorarioAlmocoDTO dto = disponibilidadeService.buscarHorarioAlmoco();
        return ResponseEntity.ok(dto);
    }


    @GetMapping
    public ResponseEntity<List<DisponibilidadeDTO>> listarDoUsuarioLogado() {
        var lista = disponibilidadeService
                .listarPorPrestadorAutenticado()
                .stream()
                .map(DisponibilidadeDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
