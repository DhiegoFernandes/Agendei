package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.CategoriaNegocio;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.dto.LatLngDTO;
import com.java360.agendei.infrastructure.dto.negocio.NegocioBuscaDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import com.java360.agendei.infrastructure.util.GeocodingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NegocioServiceBuscaTest {

    @Mock private NegocioRepository negocioRepository;
    @Mock private GeocodingService geocodingService;

    @InjectMocks private NegocioService negocioService;

    private MockedStatic<UsuarioAutenticado> usuarioAutenticadoMock;
    private MockedStatic<PermissaoUtils> permissaoUtilsMock;

    @BeforeEach void setUp() {
        usuarioAutenticadoMock = mockStatic(UsuarioAutenticado.class);
        permissaoUtilsMock = mockStatic(PermissaoUtils.class);
    }
    @AfterEach void tearDown() {
        usuarioAutenticadoMock.close();
        permissaoUtilsMock.close();
    }

    private Cliente makeClienteWithCep(int id, String cep) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setPerfil(PerfilUsuario.CLIENTE);
        c.setCep(cep);
        c.setEndereco("Rua Teste");
        c.setNumero("123");
        return c;
    }


    private Negocio makeNegocio(int id, String cep, double nota, String nome, CategoriaNegocio cat) {
        Negocio n = Negocio.builder()
                .id(id).nome(nome).cep(cep).categoria(cat).notaMedia(nota).ativo(true).build();
        return n;
    }

    @Test
    void buscarNegociosProximos_clienteSemCepFalha() {
        Cliente cliente = makeClienteWithCep(2, null);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(cliente, PerfilUsuario.CLIENTE)).thenAnswer(i->null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.buscarNegociosProximos(null, null));

        assertEquals("Cliente precisa ter endereço completo cadastrado.", ex.getMessage());
    }


    @Test
    void buscarNegociosPorAvaliacao_filtraPorNotaEOrdena() {
        Cliente cliente = makeClienteWithCep(3, "00000-000");
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(cliente, PerfilUsuario.CLIENTE)).thenAnswer(i->null);

        Negocio n1 = makeNegocio(1, "11111-111", 4.7, "A", CategoriaNegocio.SPA);
        Negocio n2 = makeNegocio(2, "22222-222", 3.9, "B", CategoriaNegocio.SPA);

        when(negocioRepository.findByAtivoTrue()).thenReturn(List.of(n1, n2));

        List<NegocioBuscaDTO> res = negocioService.buscarNegociosPorAvaliacao(4.0);

        assertTrue(res.stream().allMatch(dto -> dto.getNotaMedia() >= 4.0));
    }

    @Test
    void buscarNegociosPorAvaliacao_semNegociosRetornaListaVazia() {
        Cliente cliente = makeClienteWithCep(4, "00000-000");
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(cliente, PerfilUsuario.CLIENTE)).thenAnswer(i -> null);

        when(negocioRepository.findByAtivoTrue()).thenReturn(List.of());

        List<NegocioBuscaDTO> resultado = negocioService.buscarNegociosPorAvaliacao(4.5);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }




}
