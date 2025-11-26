package com.java360.agendei.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java360.agendei.domain.applicationservice.UsuarioService;
import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.infrastructure.controller.UsuarioController;
import com.java360.agendei.infrastructure.dto.admin.AtualizarUsuarioAdminDTO;
import com.java360.agendei.infrastructure.dto.usuario.*;
import com.java360.agendei.infrastructure.security.JwtAuthenticationFilter;
import com.java360.agendei.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UsuarioControllerTestConfig.class)
public class UsuarioControllerTest {

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    SecurityConfig securityConfig;


    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    UsuarioService usuarioService;   // MOCK CORRETO

    // ------------------------------------------------------------------------
    // POST /usuarios/registrar
    // ------------------------------------------------------------------------
    @Test
    void registrar_sucesso() throws Exception {

        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setNome("João");
        dto.setEmail("teste@test.com");
        dto.setTelefone("11987654321");
        dto.setSenha("Senha@123");
        dto.setPerfil(PerfilUsuario.CLIENTE);
        dto.setCep("12345-678");
        dto.setEndereco("Rua A");
        dto.setNumero("10");

        Cliente usuario = new Cliente();
        usuario.setId(1);
        usuario.setNome("João");
        usuario.setEmail("teste@test.com");
        dto.setTelefone("11987654321");
        usuario.setPerfil(PerfilUsuario.CLIENTE);

        Mockito.when(usuarioService.registrarUsuario(any()))
                .thenReturn(usuario);

        mockMvc.perform(post("/usuarios/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João"))
                .andExpect(jsonPath("$.email").value("teste@test.com"));
    }


    // ------------------------------------------------------------------------
    // GET /usuarios/me
    // ------------------------------------------------------------------------
    @Test
    void obterDadosUsuario_sucesso() throws Exception {

        UsuarioDetalhadoDTO dto = UsuarioDetalhadoDTO.builder()
                .id(1)
                .nome("Maria")
                .email("maria@test.com")
                .perfil(PerfilUsuario.CLIENTE)
                .build();

        Mockito.when(usuarioService.buscarDadosUsuarioPorToken(anyString()))
                .thenReturn(dto);

        mockMvc.perform(get("/usuarios/me")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"));
    }


    // ------------------------------------------------------------------------
    // GET /usuarios/todos
    // ------------------------------------------------------------------------
    @Test
    void listarUsuariosComFiltros_sucesso() throws Exception {

        UsuarioDetalhadoDTO dto = UsuarioDetalhadoDTO.builder()
                .id(1)
                .nome("Pedro")
                .email("p@test.com")
                .perfil(PerfilUsuario.CLIENTE)
                .build();

        Page<UsuarioDetalhadoDTO> pagina = new PageImpl<>(List.of(dto));

        Mockito.when(usuarioService.listarUsuariosComFiltros(any(), anyInt(), anyInt(),
                        anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(pagina);

        mockMvc.perform(get("/usuarios/todos")
                        .header("Authorization", "Bearer x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Pedro"));
    }


    // ------------------------------------------------------------------------
    // PUT /usuarios/foto-perfil
    // ------------------------------------------------------------------------
    @Test
    void atualizarFotoPerfil_sucesso() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "arquivo",
                "foto.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/usuarios/foto-perfil")
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(content().string("Foto de perfil atualizada com sucesso."));


        Mockito.verify(usuarioService).atualizarFotoPerfil(any());
    }


    // ------------------------------------------------------------------------
    // GET /usuarios/{id}/foto-perfil-url
    // ------------------------------------------------------------------------
    @Test
    void getFotoPerfilUrl_sucesso() throws Exception {

        FotoPrestadorDTO dto = new FotoPrestadorDTO(5, "Prestador XP", "/usuarios/5/foto-perfil");

        Mockito.when(usuarioService.buscarFotoPerfilDTO(5)).thenReturn(dto);

        mockMvc.perform(get("/usuarios/5/foto-perfil-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prestadorId").value(5));
    }


    // ------------------------------------------------------------------------
    // GET /usuarios/{id}/foto-perfil
    // ------------------------------------------------------------------------
    @Test
    void getFotoPerfil_sucesso() throws Exception {

        byte[] imagem = {10, 20, 30};

        Mockito.when(usuarioService.buscarFotoPerfilBytes(7))
                .thenReturn(imagem);

        mockMvc.perform(get("/usuarios/7/foto-perfil"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imagem));
    }


    // ------------------------------------------------------------------------
    // PUT /usuarios/me/atualizar
    // ------------------------------------------------------------------------
    @Test
    void atualizarPrestador_sucesso() throws Exception {

        AtualizarPrestadorDTO dto = new AtualizarPrestadorDTO();
        dto.setNome("Lucas");
        dto.setEmail("lucas@test.com");
        dto.setTelefone("11987654321");

        UsuarioDetalhadoDTO resposta = UsuarioDetalhadoDTO.builder()
                .id(20)
                .nome("Lucas")
                .email("lucas@test.com")
                .perfil(PerfilUsuario.PRESTADOR)
                .build();

        Mockito.when(usuarioService.atualizarDadosPrestador(any()))
                .thenReturn(resposta);

        mockMvc.perform(put("/usuarios/me/atualizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Lucas"));
    }


    // ------------------------------------------------------------------------
    // PATCH /usuarios/cliente
    // ------------------------------------------------------------------------
    @Test
    void atualizarCliente_sucesso() throws Exception {

        AtualizarClienteDTO dto = new AtualizarClienteDTO();
        dto.setNome("Alice");
        dto.setEmail("alice@test.com");
        dto.setTelefone("1111");
        dto.setCep("11111-111");
        dto.setEndereco("Rua B");
        dto.setNumero("123");

        UsuarioDetalhadoDTO resposta = UsuarioDetalhadoDTO.builder()
                .id(2)
                .nome("Alice")
                .email("alice@test.com")
                .perfil(PerfilUsuario.CLIENTE)
                .build();

        Mockito.when(usuarioService.atualizarDadosCliente(any()))
                .thenReturn(resposta);

        mockMvc.perform(patch("/usuarios/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alice"));
    }


    // ------------------------------------------------------------------------
    // PUT /usuarios/admin/{id}
    // ------------------------------------------------------------------------
    @Test
    void atualizarComoAdmin_sucesso() throws Exception {

        AtualizarUsuarioAdminDTO dto = new AtualizarUsuarioAdminDTO();
        dto.setNome("Admin OK");
        dto.setEmail("admin@test.com");
        dto.setTelefone("9999-4444");

        UsuarioDetalhadoDTO resposta = UsuarioDetalhadoDTO.builder()
                .id(1)
                .nome("Admin OK")
                .email("admin@test.com")
                .perfil(PerfilUsuario.ADMIN)
                .build();

        Mockito.when(usuarioService.atualizarUsuarioComoAdmin(eq(1), any()))
                .thenReturn(resposta);

        mockMvc.perform(put("/usuarios/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Admin OK"));
    }
}
