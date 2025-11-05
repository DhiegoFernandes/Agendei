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
import jakarta.transaction.Transactional;
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

    // Solicitar recuperação de senha
    public void solicitarRecuperacao(RecuperacaoSenhaRequestDTO dto) {
        String email = dto.getEmail().toLowerCase().trim();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail não encontrado."));

        // gera código de 6 dígitos
        String codigo = String.format("%06d", new Random().nextInt(999999));

        // remove códigos anteriores
        codigoRepo.deleteByEmail(email);

        CodigoRecuperacao novoCodigo = CodigoRecuperacao.builder()
                .email(email)
                .codigo(codigo)
                .expiraEm(LocalDateTime.now().plusMinutes(10))
                .build();

        codigoRepo.save(novoCodigo);

        // envia email
        emailService.enviarCodigoRecuperacao(email, codigo);
    }

    // Verificar código
    public boolean verificarCodigo(VerificarCodigoDTO dto) {
        var registro = codigoRepo.findByEmailAndCodigo(dto.getEmail(), dto.getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido."));

        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            codigoRepo.deleteByEmail(dto.getEmail());
            throw new IllegalArgumentException("Código expirado.");
        }

        return true;
    }

    // Redefinir senha
    @Transactional
    public void redefinirSenha(NovaSenhaDTO dto) {
        var registro = codigoRepo.findByEmailAndCodigo(dto.getEmail(), dto.getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido."));

        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            codigoRepo.deleteByEmail(dto.getEmail());
            throw new IllegalArgumentException("Código expirado.");
        }

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // valida a nova senha
        if (!dto.getNovaSenha().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-]).{8,}$")) {
            throw new IllegalArgumentException("A senha deve conter pelo menos 8 caracteres, incluindo uma letra maiúscula, uma minúscula, um número e um caractere especial.");
        }

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        usuarioRepository.save(usuario);

        // apaga o código após uso
        codigoRepo.deleteByEmail(dto.getEmail());
    }
}