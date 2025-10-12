package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.UsuarioService;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.infrastructure.dto.RegistroUsuarioDTO;
import com.java360.agendei.infrastructure.dto.UsuarioDTO;
import com.java360.agendei.infrastructure.dto.UsuarioDetalhadoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioDTO> registrar(@RequestBody @Valid RegistroUsuarioDTO dto) {
        Usuario usuario = usuarioService.registrarUsuario(dto);
        return ResponseEntity.ok(UsuarioDTO.fromEntity(usuario));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDetalhadoDTO> obterDadosUsuario(@RequestHeader("Authorization") String token) {
        UsuarioDetalhadoDTO usuario = usuarioService.buscarDadosUsuarioPorToken(token);
        return ResponseEntity.ok(usuario);
    }
}
