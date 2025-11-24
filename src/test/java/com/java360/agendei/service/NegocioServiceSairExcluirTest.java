package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.NegocioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NegocioServiceSairExcluirTest {

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

    private Prestador makePrestador(int id, Negocio n) {
        Prestador p = new Prestador();
        p.setId(id);
        p.setPerfil(PerfilUsuario.PRESTADOR);
        p.setNegocio(n);
        return p;
    }

    @Test
    void sairDoNegocio_deveDesvincularECancelarAgendamentos() {
        Negocio n = Negocio.builder().id(7).build();
        Prestador convidado = makePrestador(2, n);
        // dono é outro
        Prestador dono = new Prestador(); dono.setId(1); dono.setPerfil(PerfilUsuario.PRESTADOR);
        n.setCriador(dono);

        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(convidado);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(convidado, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN)).thenAnswer(i->null);

        Servico s1 = Servico.builder().id(11).prestador(convidado).negocio(n).ativo(true).build();
        when(servicoRepository.findByPrestadorIdAndNegocioId(convidado.getId(), n.getId())).thenReturn(List.of(s1));

        Agendamento pend = Agendamento.builder()
                .id(100).cliente(new Cliente()).prestador(convidado).servico(s1)
                .dataHora(LocalDateTime.now()).status(StatusAgendamento.PENDENTE).build();

        when(agendamentoRepository.findByPrestadorId(convidado.getId())).thenReturn(List.of(pend));

        negocioService.sairDoNegocio();

        // serviços devem ter sido desativados
        assertFalse(s1.isAtivo());
        // agendamento pendente cancelado
        assertEquals(StatusAgendamento.CANCELADO, pend.getStatus());
        // prestador desvinculado
        assertNull(convidado.getNegocio());
        verify(usuarioRepository).save(convidado);
    }

    @Test
    void excluirNegocio_deveDesativarTudo() {
        Prestador dono = new Prestador(); dono.setId(1); dono.setPerfil(PerfilUsuario.PRESTADOR);
        Negocio n = Negocio.builder().id(20).criador(dono).build();

        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(dono);
        permissaoUtilsMock.when(() -> PermissaoUtils.validarPermissao(dono, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN)).thenAnswer(i->null);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(dono)).thenReturn(false);

        when(negocioRepository.findById(20)).thenReturn(Optional.of(n));
        Servico s = Servico.builder().id(1).negocio(n).ativo(true).build();
        when(servicoRepository.findByNegocio_IdAndAtivoTrue(20)).thenReturn(List.of(s));

        Prestador p1 = makePrestador(2, n);
        when(prestadorRepository.findByNegocio_Id(20)).thenReturn(List.of(p1));

        Agendamento pend = Agendamento.builder().id(5).prestador(p1).servico(s).cliente(new Cliente())
                .dataHora(LocalDateTime.now()).status(StatusAgendamento.PENDENTE).build();
        when(agendamentoRepository.findByPrestadorId(p1.getId())).thenReturn(List.of(pend));

        negocioService.excluirNegocio(20);

        assertFalse(s.isAtivo());
        assertNull(p1.getNegocio());
        assertEquals(StatusAgendamento.CANCELADO, pend.getStatus());
    }
}
