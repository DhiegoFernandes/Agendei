package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.model.PlanoPrestador;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prestadores")
@RequiredArgsConstructor
public class PlanoController {

    private final UsuarioService usuarioService;

    // PUT /prestadores/{id}/plano?novoPlano=AVANCADO
    @PutMapping("/{id}/plano")
    public ResponseEntity<String> alterarPlanoPrestador(
            @PathVariable Integer id,
            @RequestParam PlanoPrestador novoPlano
    ) {
        usuarioService.alterarPlanoPrestador(id, novoPlano);
        return ResponseEntity.ok("Plano atualizado com sucesso para " + novoPlano.name());
    }
}