package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.CategoriaNegocio;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.NegocioRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.PrestadorRepository;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.infrastructure.dto.negocio.CreateNegocioDTO;
import com.java360.agendei.infrastructure.dto.negocio.NegocioDTO;
import com.java360.agendei.infrastructure.dto.negocio.UpdateNegocioDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NegocioServiceCRUDTest {

    @Mock private NegocioRepository negocioRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private NegocioService negocioService;

    private MockedStatic<UsuarioAutenticado> usuarioAutenticadoMock;
    private MockedStatic<PermissaoUtils> permissaoUtilsMock;

    @BeforeEach
    void setup() {
        usuarioAutenticadoMock = mockStatic(UsuarioAutenticado.class);
        permissaoUtilsMock = mockStatic(PermissaoUtils.class);
    }

    @AfterEach
    void tearDown() {
        usuarioAutenticadoMock.close();
        permissaoUtilsMock.close();
    }

    // Auxiliar
    private Prestador makePrestador(int id) {
        Prestador p = new Prestador();
        p.setId(id);
        p.setNome("P" + id);
        p.setPerfil(PerfilUsuario.PRESTADOR);
        return p;
    }

    private Negocio makeNegocio(int id, Prestador criador) {
        Negocio n = Negocio.builder()
                .id(id)
                .nome("Neg" + id)
                .endereco("Rua")
                .numero("1")
                .cep("00000-000")
                .categoria(CategoriaNegocio.BELEZA)
                .criador(criador)
                .ativo(true)
                .build();
        return n;
    }

    @Test
    void criarNegocio_comSucesso() {
        Prestador prestador = makePrestador(10);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(prestador, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN))
                .thenAnswer(inv -> null);

        CreateNegocioDTO dto = new CreateNegocioDTO();
        dto.setNome("MeuNeg");
        dto.setEndereco("Rua X");
        dto.setNumero("10");
        dto.setCep("12345-678");
        dto.setCategoria(CategoriaNegocio.BELEZA);

        when(negocioRepository.existsByNome(dto.getNome())).thenReturn(false);
        Negocio salvo = makeNegocio(1, prestador);
        when(negocioRepository.save(any())).thenReturn(salvo);
        when(usuarioRepository.save(prestador)).thenReturn(prestador);

        NegocioDTO res = negocioService.criarNegocio(dto);

        assertNotNull(res);
        assertEquals(salvo.getNome(), res.getNome());
        verify(negocioRepository).save(any());
        verify(usuarioRepository).save(prestador);
    }

    @Test
    void criarNegocio_falhaSeJaTemNegocio() {
        Prestador prestador = makePrestador(11);
        prestador.setNegocio(makeNegocio(99, prestador)); // já vinculado
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(prestador, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN))
                .thenAnswer(inv -> null);

        CreateNegocioDTO dto = new CreateNegocioDTO();
        dto.setNome("X");
        dto.setEndereco("Y");
        dto.setNumero("1");
        dto.setCep("00000-000");
        dto.setCategoria(CategoriaNegocio.BELEZA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.criarNegocio(dto));
        assertTrue(ex.getMessage().contains("vinculado a um negócio"));
    }

    @Test
    void atualizarNegocio_adminPodeAtualizarTudo() {
        Usuario admin = new Usuario();
        admin.setId(1);
        admin.setPerfil(PerfilUsuario.ADMIN);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(admin);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(admin, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN))
                .thenAnswer(inv -> null);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(admin)).thenReturn(true);

        Prestador criador = makePrestador(5);
        Negocio existente = makeNegocio(7, criador);
        when(negocioRepository.findById(existingId(7))).thenReturn(Optional.of(existente));
        when(negocioRepository.findById(7)).thenReturn(Optional.of(existente));

        UpdateNegocioDTO dto = new UpdateNegocioDTO();
        dto.setNome("NovoNome");
        dto.setEndereco("NovoEnd");
        dto.setNumero("2");
        dto.setCep("11111-111");
        dto.setCategoria(CategoriaNegocio.SPA);
        dto.setAtivo(true);

        when(negocioRepository.existsByNome("NovoNome")).thenReturn(false);
        when(negocioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NegocioDTO updated = negocioService.atualizarNegocio(7, dto);

        assertEquals("NovoNome", updated.getNome());
        assertEquals("NovoEnd", updated.getEndereco());
    }

    @Test
    void buscarNegocioPorId_naoEncontra() {
        when(negocioRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.buscarNegocioPorId(99));

        assertEquals("Negócio não encontrado.", ex.getMessage());
    }


    // metodo auxiliar para retornar ao id existente
    private Integer existingId(int id) { return id; }
}
