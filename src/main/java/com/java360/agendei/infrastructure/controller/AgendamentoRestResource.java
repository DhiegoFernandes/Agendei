package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AgendamentoService;
import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.infrastructure.dto.AgendamentoDTO;
import com.java360.agendei.infrastructure.dto.SaveAgendamentoDataDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static com.java360.agendei.infrastructure.controller.RestConstants.PATH_AGENDAMENTOS;


//Rest é onde primeiramente vem a requisição
@RestController
@RequestMapping(PATH_AGENDAMENTOS) // https../agendamentos (Usa constante da classe RestConstants)
@RequiredArgsConstructor
@SuppressWarnings("unused") // Tira os metodos cinzas sem usos
public class AgendamentoRestResource {

    private final AgendamentoService agendamentoService;

    @PostMapping //Criar agendamento POST
    public ResponseEntity<AgendamentoDTO> createAgendamento(@RequestBody @Valid SaveAgendamentoDataDTO saveAgendamentoData){ //Valid verifica se o objeto é valido (parametros aceitaveis)

        // Cria agendamento com dados do DTO
        Agendamento agendamento = agendamentoService.createAgendamento(saveAgendamentoData);

        // Retorna status (201 CREATED) e URI (location do postman)
        return ResponseEntity
                .created(URI.create(PATH_AGENDAMENTOS + "/" + agendamento.getId())) //retorna id
                .body(AgendamentoDTO.create(agendamento)); // retorna corpo (outros parametros)
    }

    // .../agendamentos/hshusauhsahesau (id do projeto vai na uri)
    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> loadAgendamento(@PathVariable("id") String agendamentoId){
        Agendamento agendamento = agendamentoService.loadAgendamento(agendamentoId);
        return ResponseEntity.ok(AgendamentoDTO.create(agendamento)); // 200
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgendamento(@PathVariable("id") String agendamentoId){
        agendamentoService.deleteAgendamento(agendamentoId);
        return ResponseEntity.noContent().build(); //204
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> updateAgendamento(
            @PathVariable("id") String agendamentoId, // id vem da URL
            @RequestBody @Valid SaveAgendamentoDataDTO saveAgendamentoData // pede o body da requisição e valida de acordo com a classe SaveAgendamentoDataDTO
    ){
        Agendamento agendamento = agendamentoService.updateAgendamento(agendamentoId, saveAgendamentoData);
        return ResponseEntity.ok(AgendamentoDTO.create(agendamento));
    }
}
