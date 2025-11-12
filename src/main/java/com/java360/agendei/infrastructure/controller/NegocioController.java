package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.model.CategoriaNegocio;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.infrastructure.dto.*;
import com.java360.agendei.infrastructure.dto.negocio.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/negocios")
@RequiredArgsConstructor
public class NegocioController {

    private final NegocioService negocioService;
    private final ServicoRepository servicoRepository;

    @PostMapping
    public ResponseEntity<NegocioDTO> criar(@RequestBody @Valid CreateNegocioDTO dto) {
        NegocioDTO negocio = negocioService.criarNegocio(dto);
        return ResponseEntity.ok(negocio);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NegocioDTO> atualizar(@PathVariable Integer id, @RequestBody @Valid UpdateNegocioDTO dto) {
        NegocioDTO negocio = negocioService.atualizarNegocio(id, dto);
        return ResponseEntity.ok(negocio);
    }


    //TODO testar requisição, será necessaria para acessar o id_negocio para sair dele no front-end
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

    // Sair prestador convidado
    @DeleteMapping("/sair")
    public ResponseEntity<String> sairDoNegocio() {
        negocioService.sairDoNegocio(); // ID obtido internamente via token
        return ResponseEntity.ok("Prestador removido do negócio com sucesso.");
    }

    // Sair APENAS DONO
    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluirNegocio(@PathVariable Integer id) {
        negocioService.excluirNegocio(id); // ID do solicitante extraído via token
        return ResponseEntity.ok("Negócio excluído com sucesso.");
    }

    @GetMapping("/busca-negocios")
    public ResponseEntity<List<NegocioBuscaDTO>> buscarNegociosProximos(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) CategoriaNegocio categoria
    ) {
        var lista = negocioService.buscarNegociosProximos(nome, categoria);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/avaliacao")
    public ResponseEntity<List<NegocioBuscaDTO>> buscarPorAvaliacao(
            @RequestParam(required = false) Double notaMinima
    ) {
        return ResponseEntity.ok(negocioService.buscarNegociosPorAvaliacao(notaMinima));
    }


    @GetMapping("/{id}")
    public ResponseEntity<NegocioDTO> buscarPorId(@PathVariable Integer id) {
        NegocioDTO negocio = negocioService.buscarNegocioPorId(id);
        return ResponseEntity.ok(negocio);
    }


}
