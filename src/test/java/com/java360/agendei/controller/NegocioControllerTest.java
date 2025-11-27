package com.java360.agendei.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.model.CategoriaNegocio;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.infrastructure.controller.NegocioController;
import com.java360.agendei.infrastructure.dto.ServicoDTO;
import com.java360.agendei.infrastructure.dto.negocio.*;
import com.java360.agendei.infrastructure.security.JwtAuthenticationFilter;
import com.java360.agendei.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NegocioController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança
public class NegocioControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    NegocioService negocioService;

    @MockBean
    ServicoRepository servicoRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;



    // ---------------------------------------------------------------------
    // POST /negocios
    // ---------------------------------------------------------------------
    @Test
    void criarNegocio_sucesso() throws Exception {
        CreateNegocioDTO dto = new CreateNegocioDTO();
        dto.setNome("Studio X");
        dto.setEndereco("Rua A");
        dto.setNumero("100");
        dto.setCep("12345-678");
        dto.setCategoria(CategoriaNegocio.BELEZA);

        NegocioDTO resposta = NegocioDTO.builder()
                .id(1)
                .nome("Studio X")
                .endereco("Rua A")
                .numero("100")
                .cep("12345-678")
                .categoria(CategoriaNegocio.BELEZA)
                .ativo(true)
                .build();

        Mockito.when(negocioService.criarNegocio(any())).thenReturn(resposta);

        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Studio X"));
    }

    // Teste de validação
    @Test
    void criarNegocio_erroValidacao() throws Exception {
        CreateNegocioDTO dto = new CreateNegocioDTO();
        dto.setNome(""); // inválido

        mockMvc.perform(post("/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------
    // PUT /negocios/{id}
    // ---------------------------------------------------------------------
    @Test
    void atualizarNegocio_sucesso() throws Exception {
        UpdateNegocioDTO dto = new UpdateNegocioDTO();
        dto.setNome("Novo Nome");

        NegocioDTO resposta = NegocioDTO.builder()
                .id(1)
                .nome("Novo Nome")
                .build();

        Mockito.when(negocioService.atualizarNegocio(eq(1), any()))
                .thenReturn(resposta);

        mockMvc.perform(put("/negocios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"));
    }

    // ---------------------------------------------------------------------
    // GET /negocios/negocio
    // ---------------------------------------------------------------------
    @Test
    void listarPorNegocio_sucesso() throws Exception {
        ServicoDTO servico = new ServicoDTO(
                1,
                "Corte",
                "Corte masculino",
                30.0,
                30,
                true,
                10,
                "João",
                5,
                null
        );

        Mockito.when(servicoRepository
                        .findByNegocio_NomeIgnoreCaseAndAtivoTrue("Studio X"))
                .thenReturn(List.of());

        mockMvc.perform(get("/negocios/negocio")
                        .param("nome", "Studio X"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------------
    // POST /negocios/convidar
    // ---------------------------------------------------------------------
    @Test
    void convidarPrestador_sucesso() throws Exception {
        ConviteNegocioDTO dto = new ConviteNegocioDTO();
        dto.setEmailPrestador("teste@teste.com");

        mockMvc.perform(post("/negocios/convidar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Prestador associado ao negócio com sucesso."));

        Mockito.verify(negocioService).convidarPrestadorParaNegocio(any());
    }

    // ---------------------------------------------------------------------
    // DELETE /negocios/sair
    // ---------------------------------------------------------------------
    @Test
    void sairDoNegocio_sucesso() throws Exception {

        mockMvc.perform(delete("/negocios/sair"))
                .andExpect(status().isOk())
                .andExpect(content().string("Prestador removido do negócio com sucesso."));

        Mockito.verify(negocioService).sairDoNegocio();
    }

    // ---------------------------------------------------------------------
    // DELETE /negocios/{id}
    // ---------------------------------------------------------------------
    @Test
    void excluirNegocio_sucesso() throws Exception {

        mockMvc.perform(delete("/negocios/5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Negócio excluído com sucesso."));

        Mockito.verify(negocioService).excluirNegocio(5);
    }

    // ---------------------------------------------------------------------
    // GET /negocios/busca-negocios
    // ---------------------------------------------------------------------
    @Test
    void buscarNegociosProximos_sucesso() throws Exception {
        NegocioBuscaDTO dto = NegocioBuscaDTO.builder()
                .id(1)
                .nome("Studio X")
                .distanciaKm(2.5)
                .build();

        Mockito.when(negocioService.buscarNegociosProximos(any(), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/negocios/busca-negocios")
                        .param("nome", "Studio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Studio X"));
    }

    // ---------------------------------------------------------------------
    // GET /negocios/avaliacao
    // ---------------------------------------------------------------------
    @Test
    void buscarPorAvaliacao_sucesso() throws Exception {
        NegocioBuscaDTO dto = NegocioBuscaDTO.builder()
                .id(2)
                .nome("Barbearia Top")
                .notaMedia(4.8)
                .build();

        Mockito.when(negocioService.buscarNegociosPorAvaliacao(4.0))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/negocios/avaliacao")
                        .param("notaMinima", "4.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Barbearia Top"));
    }

    // ---------------------------------------------------------------------
    // GET /negocios/{id}
    // ---------------------------------------------------------------------
    @Test
    void buscarPorId_sucesso() throws Exception {
        NegocioDTO dto = NegocioDTO.builder()
                .id(1)
                .nome("Studio X")
                .build();

        Mockito.when(negocioService.buscarNegocioPorId(1))
                .thenReturn(dto);

        mockMvc.perform(get("/negocios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Studio X"));
    }

    // ---------------------------------------------------------------------
    // GET /negocios/todos
    // ---------------------------------------------------------------------
    @Test
    void listarTodosNegocios_sucesso() throws Exception {
        NegocioResumoDTO dto = NegocioResumoDTO.builder()
                .id(1)
                .nome("Studio X")
                .build();

        Mockito.when(negocioService.listarTodosNegocios())
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/negocios/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Studio X"));
    }

    // ---------------------------------------------------------------------
    // GET /negocios/buscar-por-nome
    // ---------------------------------------------------------------------
    @Test
    void buscarPorNome_sucesso() throws Exception {
        NegocioResumoDTO dto = NegocioResumoDTO.builder()
                .id(1)
                .nome("Studio X")
                .build();

        Mockito.when(negocioService.buscarNegociosPorNome("Studio"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/negocios/buscar-por-nome")
                        .param("nome", "Studio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Studio X"));
    }
}
