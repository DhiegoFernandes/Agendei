package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PlanoPrestador;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.dto.negocio.ConviteNegocioDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NegocioServiceConviteTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PrestadorRepository prestadorRepository;

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

    private Prestador makePrestador(int id, Negocio negocio, PlanoPrestador plano) {
        Prestador p = new Prestador();
        p.setId(id);
        p.setPerfil(PerfilUsuario.PRESTADOR);
        p.setNegocio(negocio);
        p.setPlano(plano);
        return p;
    }

    @Test
    void convidarPrestador_sucesso() {
        // dono do negocio que convida
        Negocio n = Negocio.builder().id(5).nome("N").build();
        Prestador dono = makePrestador(1, n, PlanoPrestador.INTERMEDIARIO);
        n.setCriador(dono);

        Usuario convidado = new Prestador();
        ((Prestador) convidado).setNegocio(null);
        ((Prestador) convidado).setId(20);

        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(dono);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(dono, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN)).thenAnswer(i->null);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(dono)).thenReturn(false);

        ConviteNegocioDTO dto = new ConviteNegocioDTO();
        dto.setEmailPrestador("CONVIDADO@EXAMPLE.COM");

        when(prestadorRepository.findByNegocio_Id(n.getId())).thenReturn(List.of(dono)); // só dono -> quantidadeAtual = 0
        when(usuarioRepository.findByEmail("convidado@example.com")).thenReturn(Optional.of(convidado));
        doAnswer(inv -> {
            return convidado;
        }).when(usuarioRepository).save(any());

        negocioService.convidarPrestadorParaNegocio(dto);

        verify(usuarioRepository).findByEmail("convidado@example.com");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void convidarPrestador_falhaLimitePlano() {
        Negocio n = Negocio.builder().id(5).build();
        Prestador dono = makePrestador(2, n, PlanoPrestador.BASICO); // limiteConvites = 1
        n.setCriador(dono);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(dono);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(dono, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN)).thenAnswer(i->null);

        when(prestadorRepository.findByNegocio_Id(n.getId())).thenReturn(List.of(dono, new Prestador())); // tamanho 2
        ConviteNegocioDTO dto = new ConviteNegocioDTO();
        dto.setEmailPrestador("a@b.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.convidarPrestadorParaNegocio(dto));
        assertTrue(ex.getMessage().contains("permite no máximo"));
    }

    @Test
    void convidarPrestador_falhaSeEmailNaoExiste() {
        Negocio n = Negocio.builder().id(8).build();
        Prestador dono = makePrestador(3, n, PlanoPrestador.AVANCADO);
        n.setCriador(dono);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(dono);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(dono, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN)).thenAnswer(i->null);

        when(prestadorRepository.findByNegocio_Id(n.getId())).thenReturn(List.of(dono));
        when(usuarioRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());

        ConviteNegocioDTO dto = new ConviteNegocioDTO();
        dto.setEmailPrestador("x@y.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.convidarPrestadorParaNegocio(dto));
        assertTrue(ex.getMessage().contains("não existe"));
    }
}
