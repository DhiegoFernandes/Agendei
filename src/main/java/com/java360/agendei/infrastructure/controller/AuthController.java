package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AuthService;
import com.java360.agendei.infrastructure.dto.NovaSenhaDTO;
import com.java360.agendei.infrastructure.dto.RecuperacaoSenhaRequestDTO;
import com.java360.agendei.infrastructure.dto.VerificarCodigoDTO;
import com.java360.agendei.infrastructure.dto.usuario.AuthRequestDTO;
import com.java360.agendei.infrastructure.dto.usuario.AuthResponseDTO;
import com.java360.agendei.infrastructure.dto.usuario.TokenRequestDTO;
import com.java360.agendei.infrastructure.dto.usuario.TokenValidationResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthRequestDTO dto) {
        AuthResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponseDTO> validateToken(@RequestBody @Valid TokenRequestDTO dto) {
        boolean valido = authService.isTokenValid(dto.getToken());
        return ResponseEntity.ok(new TokenValidationResponseDTO(valido));
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<String> solicitarRecuperacao(@RequestBody @Valid RecuperacaoSenhaRequestDTO dto) {
        authService.solicitarRecuperacao(dto);
        return ResponseEntity.ok("Código de recuperação enviado para o e-mail.");
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<String> verificarCodigo(@RequestBody @Valid VerificarCodigoDTO dto) {
        authService.verificarCodigo(dto);
        return ResponseEntity.ok("Código válido.");
    }

    @PostMapping("/nova-senha")
    public ResponseEntity<String> redefinirSenha(@RequestBody @Valid NovaSenhaDTO dto) {
        authService.redefinirSenha(dto);
        return ResponseEntity.ok("Senha redefinida com sucesso.");
    }

}
