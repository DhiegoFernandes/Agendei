package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AuthService;
import com.java360.agendei.infrastructure.dto.AuthRequestDTO;
import com.java360.agendei.infrastructure.dto.AuthResponseDTO;
import com.java360.agendei.infrastructure.dto.TokenRequestDTO;
import com.java360.agendei.infrastructure.dto.TokenValidationResponseDTO;
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
}
