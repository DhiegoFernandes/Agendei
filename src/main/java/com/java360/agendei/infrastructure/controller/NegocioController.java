package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.infrastructure.dto.ConviteNegocioDTO;
import com.java360.agendei.infrastructure.dto.CreateNegocioDTO;
import com.java360.agendei.infrastructure.dto.NegocioDTO;
import com.java360.agendei.infrastructure.dto.ServicoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/negocios")
@RequiredArgsConstructor
public class NegocioController {

    private final NegocioService negocioService;
    private final ServicoRepository servicoRepository;

    @PostMapping
    public ResponseEntity<NegocioDTO> criar(@RequestBody @Valid CreateNegocioDTO dto) {
        Negocio negocio = negocioService.criarNegocio(dto);
        return ResponseEntity
                .created(URI.create("/negocios/" + negocio.getId()))
                .body(NegocioDTO.fromEntity(negocio));
    }

    @GetMapping("/negocio")
    public ResponseEntity<List<ServicoDTO>> listarPorNegocio(@RequestParam String nome) {
        var lista = servicoRepository.findByNegocio_NomeIgnoreCaseAndAtivoTrue(nome)
                .stream()
                .map(ServicoDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/convidar")
    public ResponseEntity<String> convidarPrestador(@RequestBody @Valid ConviteNegocioDTO dto) {
        negocioService.convidarPrestadorParaNegocio(dto);
        return ResponseEntity.ok("Prestador associado ao negócio com sucesso.");
    }

    @DeleteMapping("/sair")
    public ResponseEntity<String> sairDoNegocio() {
        negocioService.sairDoNegocio(); // ID obtido internamente via token
        return ResponseEntity.ok("Prestador removido do negócio com sucesso.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluirNegocio(@PathVariable Integer id) {
        negocioService.excluirNegocio(id); // ID do solicitante extraído via token
        return ResponseEntity.ok("Negócio excluído com sucesso.");
    }

}
