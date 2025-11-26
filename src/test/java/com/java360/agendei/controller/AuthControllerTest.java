package com.java360.agendei.controller;

import com.java360.agendei.domain.applicationservice.AuthService;
import com.java360.agendei.infrastructure.controller.AuthController;
import com.java360.agendei.infrastructure.dto.NovaSenhaDTO;
import com.java360.agendei.infrastructure.dto.RecuperacaoSenhaRequestDTO;
import com.java360.agendei.infrastructure.dto.VerificarCodigoDTO;
import com.java360.agendei.infrastructure.dto.usuario.AuthRequestDTO;
import com.java360.agendei.infrastructure.dto.usuario.AuthResponseDTO;
import com.java360.agendei.infrastructure.dto.usuario.TokenRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        mapper = new ObjectMapper();
    }

    @Test
    void login_sucesso() throws Exception {

        AuthResponseDTO response = new AuthResponseDTO("abc123", "cliente", "João");
        when(authService.login(any(AuthRequestDTO.class))).thenReturn(response);

        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("teste@email.com");
        request.setSenha("123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc123"))
                .andExpect(jsonPath("$.perfil").value("cliente"))
                .andExpect(jsonPath("$.nome").value("João"));

        verify(authService).login(any(AuthRequestDTO.class));
    }

    @Test
    void validateToken_sucesso() throws Exception {

        TokenRequestDTO request = new TokenRequestDTO();
        request.setToken("abc");

        when(authService.isTokenValid("abc")).thenReturn(true);

        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).isTokenValid("abc");
    }

    @Test
    void solicitarRecuperacao_sucesso() throws Exception {

        RecuperacaoSenhaRequestDTO dto = new RecuperacaoSenhaRequestDTO();
        dto.setEmail("teste@teste.com");

        mockMvc.perform(post("/auth/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Código de recuperação enviado para o e-mail."));

        verify(authService).solicitarRecuperacao(any(RecuperacaoSenhaRequestDTO.class));
    }

    // -------------------------------------------------------------
    // /auth/verificar-codigo
    // -------------------------------------------------------------
    @Test
    void verificarCodigo_sucesso() throws Exception {

        VerificarCodigoDTO dto = new VerificarCodigoDTO();
        dto.setEmail("t@t.com");
        dto.setCodigo("111111");

        mockMvc.perform(post("/auth/verificar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Código válido."));

        verify(authService).verificarCodigo(any(VerificarCodigoDTO.class));
    }

    @Test
    void redefinirSenha_sucesso() throws Exception {

        NovaSenhaDTO dto = new NovaSenhaDTO();
        dto.setEmail("t@t.com");
        dto.setCodigo("777777");
        dto.setNovaSenha("Senha@123");

        mockMvc.perform(post("/auth/nova-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Senha redefinida com sucesso."));

        verify(authService).redefinirSenha(any(NovaSenhaDTO.class));
    }
}
