package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.usuario.AuthRequestDTO;
import com.java360.agendei.infrastructure.dto.usuario.AuthResponseDTO;
import com.java360.agendei.infrastructure.exception.RequestException;
import com.java360.agendei.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDTO login(AuthRequestDTO dto) {
        String emailNormalizado = dto.getEmail().toLowerCase().trim();
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new RequestException("AuthError", "Email ou senha inválidos"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new RequestException("AuthError", "Email ou senha inválidos");
        }

        String token = jwtService.generateToken(usuario.getId());
        return new AuthResponseDTO(  token,
                usuario.getPerfil().name().toLowerCase(), // converte para minúscula
                usuario.getNome());
    }

    public boolean isTokenValid(String token) {
        return jwtService.isValid(token);
    }
}