package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.AuthRequestDTO;
import com.java360.agendei.infrastructure.exception.RequestException;
import com.java360.agendei.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String login(AuthRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RequestException("AuthError", "Email ou senha inválidos"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new RequestException("AuthError", "Email ou senha inválidos");
        }

        return jwtService.generateToken(usuario.getId());
    }
}