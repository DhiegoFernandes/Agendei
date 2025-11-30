package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.AuthService;
import com.java360.agendei.domain.entity.CodigoRecuperacao;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private CodigoRecuperacaoRepository codigoRepo;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("teste@agendei.com");
        usuario.setSenha("encoded_pass");
        usuario.setNome("João");
        usuario.setPerfil(PerfilUsuario.CLIENTE);
    }

    @Test
    void login_sucesso() {
        AuthRequestDTO dto = new AuthRequestDTO();
        dto.setEmail("TESTE@agendei.com ");
        dto.setSenha("123");

        when(usuarioRepository.findByEmail("teste@agendei.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123", "encoded_pass")).thenReturn(true);
        when(jwtService.generateToken(10)).thenReturn("token123");

        AuthResponseDTO result = authService.login(dto);

        assertEquals("token123", result.getToken());
        assertEquals("cliente", result.getPerfil());
        assertEquals("João", result.getNome());
    }

    @Test
    void login_emailNaoEncontrado_lancaErro() {
        AuthRequestDTO dto = new AuthRequestDTO();
        dto.setEmail("x@x.com");
        dto.setSenha("123");

        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(RequestException.class, () -> authService.login(dto));
    }

    @Test
    void login_senhaInvalida_lancaErro() {
        AuthRequestDTO dto = new AuthRequestDTO();
        dto.setEmail("a@a.com");
        dto.setSenha("errada");

        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", usuario.getSenha())).thenReturn(false);

        assertThrows(RequestException.class, () -> authService.login(dto));
    }

    @Test
    void isTokenValid_sucesso() {
        when(jwtService.isValid("abc")).thenReturn(true);
        assertTrue(authService.isTokenValid("abc"));
    }

    @Test
    void isTokenValid_invalido() {
        when(jwtService.isValid("abc")).thenReturn(false);
        assertFalse(authService.isTokenValid("abc"));
    }


    @Test
    void solicitarRecuperacao_sucesso() {
        RecuperacaoSenhaRequestDTO dto = new RecuperacaoSenhaRequestDTO();
        dto.setEmail("teste@agendei.com");

        when(usuarioRepository.findByEmail("teste@agendei.com"))
                .thenReturn(Optional.of(usuario));

        authService.solicitarRecuperacao(dto);

        verify(codigoRepo, times(1)).save(any(CodigoRecuperacao.class));
        verify(emailService, times(1))
                .enviarCodigoRecuperacao(eq("teste@agendei.com"), any());
    }

    @Test
    void solicitarRecuperacao_emailNaoEncontrado() {
        RecuperacaoSenhaRequestDTO dto = new RecuperacaoSenhaRequestDTO();
        dto.setEmail("x@x.com");

        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.solicitarRecuperacao(dto));
    }

    @Test
    void verificarCodigo_sucesso() {
        VerificarCodigoDTO dto = new VerificarCodigoDTO();
        dto.setEmail("teste@agendei.com");
        dto.setCodigo("111111");

        CodigoRecuperacao registro = CodigoRecuperacao.builder()
                .email("teste@agendei.com")
                .codigo("111111")
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusMinutes(5))
                .ativo(true)
                .build();

        when(codigoRepo.findByEmailAndCodigo("teste@agendei.com", "111111"))
                .thenReturn(Optional.of(registro));

        assertTrue(authService.verificarCodigo(dto));
    }

    @Test
    void verificarCodigo_codigoInvalido() {
        VerificarCodigoDTO dto = new VerificarCodigoDTO();
        dto.setEmail("a@a.com");
        dto.setCodigo("123");

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.verificarCodigo(dto));
    }

    @Test
    void verificarCodigo_expirado() {
        VerificarCodigoDTO dto = new VerificarCodigoDTO();
        dto.setEmail("a@a.com");
        dto.setCodigo("123");

        CodigoRecuperacao registro = CodigoRecuperacao.builder()
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMinutes(1))
                .ativo(true)
                .build();

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.of(registro));

        assertThrows(IllegalArgumentException.class,
                () -> authService.verificarCodigo(dto));
    }

    @Test
    void verificarCodigo_inativo() {
        VerificarCodigoDTO dto = new VerificarCodigoDTO();
        dto.setEmail("a@a.com");
        dto.setCodigo("123");

        CodigoRecuperacao registro = CodigoRecuperacao.builder()
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusMinutes(5))
                .ativo(false)
                .build();

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.of(registro));

        assertThrows(IllegalArgumentException.class,
                () -> authService.verificarCodigo(dto));
    }

    @Test
    void redefinirSenha_sucesso() {
        NovaSenhaDTO dto = new NovaSenhaDTO();
        dto.setEmail("teste@agendei.com");
        dto.setCodigo("999999");
        dto.setNovaSenha("Aa1@aaaa");

        CodigoRecuperacao registro = CodigoRecuperacao.builder()
                .email("teste@agendei.com")
                .codigo("999999")
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusMinutes(10))
                .ativo(true)
                .build();

        when(codigoRepo.findByEmailAndCodigo("teste@agendei.com", "999999"))
                .thenReturn(Optional.of(registro));

        when(usuarioRepository.findByEmail("teste@agendei.com"))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.encode("Aa1@aaaa"))
                .thenReturn("encoded");

        authService.redefinirSenha(dto);

        verify(usuarioRepository).save(usuario);
        verify(codigoRepo).save(registro);
        assertFalse(registro.isAtivo());
    }

    @Test
    void redefinirSenha_codigoInvalido() {
        NovaSenhaDTO dto = new NovaSenhaDTO();
        dto.setEmail("x@x.com");
        dto.setCodigo("123");
        dto.setNovaSenha("Aa1@aaaa");

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.redefinirSenha(dto));
    }

    @Test
    void redefinirSenha_codigoExpirado() {
        NovaSenhaDTO dto = new NovaSenhaDTO();
        dto.setEmail("x@x.com");
        dto.setCodigo("123");
        dto.setNovaSenha("Aa1@aaaa");

        CodigoRecuperacao reg = CodigoRecuperacao.builder()
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusMinutes(1))
                .ativo(true)
                .build();

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.of(reg));

        assertThrows(IllegalArgumentException.class,
                () -> authService.redefinirSenha(dto));
    }

    @Test
    void redefinirSenha_codigoInativo() {
        NovaSenhaDTO dto = new NovaSenhaDTO();
        dto.setEmail("x@x.com");
        dto.setCodigo("123");
        dto.setNovaSenha("Aa1@aaaa");

        CodigoRecuperacao reg = CodigoRecuperacao.builder()
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusMinutes(1))
                .ativo(false)
                .build();

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.of(reg));

        assertThrows(IllegalArgumentException.class,
                () -> authService.redefinirSenha(dto));
    }

    @Test
    void redefinirSenha_usuarioNaoEncontrado() {
        NovaSenhaDTO dto = new NovaSenhaDTO();
        dto.setEmail("x@x.com");
        dto.setCodigo("123");
        dto.setNovaSenha("Aa1@aaaa");

        CodigoRecuperacao reg = CodigoRecuperacao.builder()
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusMinutes(1))
                .ativo(true)
                .build();

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.of(reg));

        when(usuarioRepository.findByEmail(any()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.redefinirSenha(dto));
    }

    @Test
    void redefinirSenha_senhaFraca() {
        NovaSenhaDTO dto = new NovaSenhaDTO();
        dto.setEmail("x@x.com");
        dto.setCodigo("123");
        dto.setNovaSenha("123"); // fraca

        CodigoRecuperacao reg = CodigoRecuperacao.builder()
                .expiraEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusMinutes(1))
                .ativo(true)
                .build();

        when(codigoRepo.findByEmailAndCodigo(any(), any()))
                .thenReturn(Optional.of(reg));

        when(usuarioRepository.findByEmail(any()))
                .thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> authService.redefinirSenha(dto));
    }
}
