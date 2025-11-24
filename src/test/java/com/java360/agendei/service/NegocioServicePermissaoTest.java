package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.*;
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
class NegocioServicePermissaoTest {

    @Mock private NegocioRepository negocioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private PrestadorRepository prestadorRepository;
    @Mock private AgendamentoRepository agendamentoRepository;

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

    @Test
    void atualizarNegocio_semPermissaoLancaseErro() {
        // usuario que nao e dono nem admin
        Prestador prestador = new Prestador();
        prestador.setId(2);
        prestador.setPerfil(PerfilUsuario.PRESTADOR);

        Usuario dono = new Usuario();
        dono.setId(99);

        Negocio n = Negocio.builder().id(10).criador(new Prestador(){{
            setId(1);
        }}).ativo(true).build();

        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(prestador, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN))
                .thenAnswer(i->null);
        when(negocioRepository.findById(10)).thenReturn(Optional.of(n));

        UpdateNegocioDTO dto = new UpdateNegocioDTO();
        dto.setNome("X");

        SecurityException ex = assertThrows(SecurityException.class,
                () -> negocioService.atualizarNegocio(10, dto));
        assertTrue(ex.getMessage().contains("permissão"));
    }

    @Test
    void atualizarNegocio_prestadorDonoNaoPodeAlterarCamposRestringidos() {
        Prestador dono = new Prestador();
        dono.setId(5);
        dono.setPerfil(PerfilUsuario.PRESTADOR);

        Negocio n = Negocio.builder().id(50).criador(dono).ativo(true).build();

        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(dono);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(dono, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN)).thenAnswer(i->null);
        when(negocioRepository.findById(50)).thenReturn(Optional.of(n));

        UpdateNegocioDTO dto = new UpdateNegocioDTO();
        dto.setEndereco("Outro");
        dto.setCep("11111-111");

        // dono não admin tentando alterar endereco -> SecurityException
        SecurityException ex = assertThrows(SecurityException.class,
                () -> negocioService.atualizarNegocio(50, dto));
        assertTrue(ex.getMessage().contains("Prestadores só podem alterar"));
    }
}
