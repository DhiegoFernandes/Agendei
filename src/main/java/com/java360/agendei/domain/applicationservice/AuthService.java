package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.CodigoRecuperacao;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.CodigoRecuperacaoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.NovaSenhaDTO;
import com.java360.agendei.infrastructure.dto.RecuperacaoSenhaRequestDTO;
import com.java360.agendei.infrastructure.dto.VerificarCodigoDTO;
import com.java360.agendei.infrastructure.dto.usuario.AuthRequestDTO;
import com.java360.agendei.infrastructure.dto.usuario.AuthResponseDTO;
import com.java360.agendei.infrastructure.email.EmailService;
import com.java360.agendei.infrastructure.exception.RequestException;
import com.java360.agendei.infrastructure.security.JwtService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CodigoRecuperacaoRepository codigoRepo;
    private final EmailService emailService;

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

    // --- Solicitar recuperação ---
    @Transactional
    public void solicitarRecuperacao(RecuperacaoSenhaRequestDTO dto) {
        String email = dto.getEmail().toLowerCase().trim();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail não encontrado."));

        String codigo = String.format("%06d", new Random().nextInt(999999));

        // cria novo registro (sem deletar o anterior)
        CodigoRecuperacao novoCodigo = CodigoRecuperacao.builder()
                .email(email)
                .codigo(codigo)
                .expiraEm(LocalDateTime.now().plusMinutes(10))
                .ativo(true)
                .build();

        codigoRepo.save(novoCodigo);

        emailService.enviarCodigoRecuperacao(email, codigo);
    }

    // --- Verificar código ---
    @Transactional(readOnly = true)
    public boolean verificarCodigo(VerificarCodigoDTO dto) {
        var registro = codigoRepo.findByEmailAndCodigo(dto.getEmail(), dto.getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido."));

        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código expirado.");
        }

        if (!registro.isAtivo()) {
            throw new IllegalArgumentException("Código já utilizado.");
        }

        return true;
    }

    // --- Redefinir senha ---
    @Transactional
    public void redefinirSenha(NovaSenhaDTO dto) {
        var registro = codigoRepo.findByEmailAndCodigo(dto.getEmail(), dto.getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido."));

        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código expirado.");
        }

        if (!registro.isAtivo()) {
            throw new IllegalArgumentException("Código já utilizado.");
        }

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!dto.getNovaSenha().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-]).{8,}$")) {
            throw new IllegalArgumentException("A senha deve conter pelo menos 8 caracteres, incluindo uma letra maiúscula, uma minúscula, um número e um caractere especial.");
        }

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        usuarioRepository.save(usuario);

        // marca o código como utilizado (não exclui)
        registro.setAtivo(false);
        codigoRepo.save(registro);
    }
}