package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.ServicoService;
import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.infrastructure.dto.HorariosDisponiveisDTO;
import com.java360.agendei.infrastructure.dto.SaveServicoDTO;
import com.java360.agendei.infrastructure.dto.ServicoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.java360.agendei.infrastructure.controller.RestConstants.PATH_SERVICOS;

@RestController
@RequestMapping(PATH_SERVICOS)
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    @PostMapping
    public ResponseEntity<ServicoDTO> cadastrar(@RequestBody @Valid SaveServicoDTO dto) {
        System.out.println("post");
        Servico servico = servicoService.cadastrarServico(dto);
        return ResponseEntity
                .created(URI.create(PATH_SERVICOS + "/" + servico.getId()))
                .body(ServicoDTO.fromEntity(servico));
    }

    @GetMapping("/{id}/horarios-disponiveis")
    public ResponseEntity<HorariosDisponiveisDTO> listarHorarios(@PathVariable("id") String servicoId) {
        var dto = servicoService.listarHorariosPorServico(servicoId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<ServicoDTO>> listarAtivos() {
        System.out.println("get");
        List<ServicoDTO> lista = servicoService
                .listarServicosAtivos()
                .stream()
                .map(ServicoDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicoDTO> atualizar(@PathVariable String id, @RequestBody @Valid SaveServicoDTO dto) {
        System.out.println("atualizar put");
        Servico servico = servicoService.atualizarServico(id, dto);
        return ResponseEntity.ok(ServicoDTO.fromEntity(servico));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        System.out.println("atualizar put");
        servicoService.desativarServico(id);
        return ResponseEntity.noContent().build();
    }
}
