package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.AuthService;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.AuthRequestDTO;
import com.java360.agendei.infrastructure.dto.AuthResponseDTO;
import com.java360.agendei.infrastructure.security.JwtService;
import com.java360.agendei.infrastructure.util.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthRequestDTO dto) {
        String token = authService.login(dto);
        return ResponseEntity.ok(new AuthResponseDTO(token));
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<String> recuperarSenha(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Gera token de 15 min
        String token = jwtService.generateResetToken(usuario.getId());

        // Link para o front
        String link = "http://localhost:3000/resetar-senha?token=" + token;

        emailService.enviarEmailRecuperacao(usuario.getEmail(), usuario.getNome(), link);

        return ResponseEntity.ok("E-mail de recuperação enviado!");
    }

    @PostMapping("/resetar-senha")
    public ResponseEntity<String> resetarSenha(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String novaSenha = payload.get("novaSenha");

        if (!jwtService.isValid(token)) {
            return ResponseEntity.badRequest().body("Token inválido ou expirado");
        }

        Integer userId = jwtService.extractUserId(token);

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        return ResponseEntity.ok("Senha redefinida com sucesso!");
    }



}
