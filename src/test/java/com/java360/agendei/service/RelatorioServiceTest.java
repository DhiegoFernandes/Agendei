package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.RelatorioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.dto.admin.ResumoAdministrativoDTO;
import com.java360.agendei.infrastructure.dto.relatorios.*;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RelatorioServiceTest {

    private AgendamentoRepository agRepo;
    private NegocioRepository negRepo;
    private PrestadorRepository prestRepo;
    private UsuarioRepository userRepo;
    private ServicoRepository servRepo;

    private RelatorioService service;

    private MockedStatic<UsuarioAutenticado> auth;

    private Prestador mockPrestador() {
        Prestador p = new Prestador();
        p.setId(1);
        p.setNome("Prestador");
        p.setPerfil(PerfilUsuario.PRESTADOR);
        return p;
    }

    private Usuario mockAdmin() {
        Usuario u = new Usuario();
        u.setId(99);
        u.setPerfil(PerfilUsuario.ADMIN);
        return u;
    }

    @BeforeEach
    void setup() {
        agRepo = mock(AgendamentoRepository.class);
        negRepo = mock(NegocioRepository.class);
        prestRepo = mock(PrestadorRepository.class);
        userRepo = mock(UsuarioRepository.class);
        servRepo = mock(ServicoRepository.class);

        service = new RelatorioService(agRepo, negRepo, prestRepo, userRepo, servRepo);
        auth = Mockito.mockStatic(UsuarioAutenticado.class);
    }

    @AfterEach
    void close() {
        auth.close();
    }

    @Test
    void getPrestadorId_sucesso() {
        Prestador p = mockPrestador();
        auth.when(UsuarioAutenticado::get).thenReturn(p);

        YearMonth mes = YearMonth.of(2025, 1);
        when(agRepo.findByPrestadorIdAndDataHoraBetween(anyInt(), any(), any()))
                .thenReturn(List.of());

        var r = service.relatorioFinanceiroMensal(mes);
        assertNotNull(r);
    }

    @Test
    void getPrestadorId_usuarioInvalido() {
        Usuario u = new Usuario();
        u.setPerfil(PerfilUsuario.CLIENTE);

        auth.when(UsuarioAutenticado::get).thenReturn(u);

        assertThrows(SecurityException.class, () ->
                service.evolucaoMensal(2025));
    }


    // relatorio financeiro
    @Test
    void relatorioFinanceiroMensal_sucesso() {
        Prestador p = mockPrestador();
        auth.when(UsuarioAutenticado::get).thenReturn(p);

        Servico serv = new Servico();
        serv.setValor(100);

        Agendamento a1 = new Agendamento();
        a1.setPrestador(p);
        a1.setServico(serv);
        a1.setStatus(StatusAgendamento.CONCLUIDO);
        a1.setDataHora(LocalDateTime.now());

        Agendamento a2 = new Agendamento();
        a2.setPrestador(p);
        a2.setServico(serv);
        a2.setStatus(StatusAgendamento.CANCELADO);
        a2.setDataHora(LocalDateTime.now());

        when(agRepo.findByPrestadorIdAndDataHoraBetween(anyInt(), any(), any()))
                .thenReturn(List.of(a1, a2));

        RelatorioFinanceiroDTO dto = service.relatorioFinanceiroMensal(YearMonth.now());

        assertEquals(new BigDecimal("200.0"), dto.getGanhosEsperados());
        assertEquals(0, dto.getGanhosRealizados().compareTo(new BigDecimal("100")));
        assertEquals(50.0, dto.getTaxaCancelamentos());
    }

    // evolucao Mensal
    @Test
    void evolucaoMensal_sucesso() {
        Prestador p = mockPrestador();
        auth.when(UsuarioAutenticado::get).thenReturn(p);

        when(agRepo.findByPrestadorIdAndStatusAndDataHoraBetween(anyInt(), eq(StatusAgendamento.CONCLUIDO), any(), any()))
                .thenReturn(List.of());

        var lista = service.evolucaoMensal(2025);
        assertEquals(12, lista.size());
    }


    // evolucao anual
    @Test
    void evolucaoAnual_sucesso() {
        Prestador p = mockPrestador();
        auth.when(UsuarioAutenticado::get).thenReturn(p);

        when(agRepo.findByPrestadorIdAndStatusAndDataHoraBetween(anyInt(), eq(StatusAgendamento.CONCLUIDO), any(), any()))
                .thenReturn(List.of());

        var lista = service.evolucaoAnual(2023, 2025);
        assertEquals(3, lista.size());
    }

    // servicos mais  vendidos
    @Test
    void servicosMaisVendidos_sucesso() {
        Prestador p = mockPrestador();
        auth.when(UsuarioAutenticado::get).thenReturn(p);

        Servico s = new Servico();
        s.setValor(50);
        s.setTitulo("Corte");

        Agendamento a = new Agendamento();
        a.setPrestador(p);
        a.setServico(s);
        a.setStatus(StatusAgendamento.CONCLUIDO);
        a.setDataHora(LocalDateTime.now());

        when(agRepo.findByPrestadorIdAndStatusAndDataHoraBetween(anyInt(), eq(StatusAgendamento.CONCLUIDO), any(), any()))
                .thenReturn(List.of(a));

        var result = service.servicosMaisVendidos(YearMonth.now());
        assertEquals(1, result.size());
        assertEquals("Corte", result.get(0).getTituloServico());
    }

    // resumo administrativo
    @Test
    void resumoAdministrativo_sucesso() {
        auth.when(UsuarioAutenticado::get).thenReturn(mockAdmin());

        when(userRepo.countByPerfil(PerfilUsuario.PRESTADOR)).thenReturn(10L);
        when(userRepo.countByPerfil(PerfilUsuario.CLIENTE)).thenReturn(20L);
        when(servRepo.countByAtivoTrue()).thenReturn(5L);
        when(negRepo.countByAtivoTrue()).thenReturn(3L);
        when(agRepo.count()).thenReturn(100L);

        ResumoAdministrativoDTO dto = service.resumoAdministrativo();

        assertEquals(10, dto.getTotalPrestadores());
        assertEquals(20, dto.getTotalClientes());
        assertEquals(5, dto.getTotalServicosAtivos());
        assertEquals(3, dto.getTotalNegociosAtivos());
        assertEquals(100, dto.getTotalAgendamentos());
    }

    @Test
    void relatorioNegocio_sucesso() {
        Prestador dono = mockPrestador();
        auth.when(UsuarioAutenticado::get).thenReturn(dono);

        Negocio n = new Negocio();
        n.setId(10);
        n.setNome("Studio X");
        n.setCriador(dono);

        Prestador prest1 = new Prestador();
        prest1.setId(1);
        prest1.setNome("João");
        prest1.setNegocio(n);

        Servico s = new Servico();
        s.setValor(100);

        Agendamento ag = new Agendamento();
        ag.setPrestador(prest1);
        ag.setServico(s);
        ag.setStatus(StatusAgendamento.CONCLUIDO);
        ag.setDataHora(LocalDateTime.now());

        when(negRepo.findById(10)).thenReturn(Optional.of(n));
        when(prestRepo.findByNegocio_Id(10)).thenReturn(List.of(prest1));
        when(agRepo.findAll()).thenReturn(List.of(ag));

        RelatorioNegocioDTO dto = service.relatorioNegocio(10, YearMonth.now());

        assertEquals("Studio X", dto.getNomeNegocio());
        assertEquals(1, dto.getTotalServicos());
        assertEquals(0, dto.getGanhosTotais().compareTo(new BigDecimal("100")));
        assertEquals(1, dto.getPrestadores().size());
    }

    @Test
    void relatorioNegocio_usuarioNaoDono() {
        Usuario u = new Usuario();
        u.setId(2);
        u.setPerfil(PerfilUsuario.PRESTADOR);

        auth.when(UsuarioAutenticado::get).thenReturn(u);

        Prestador dono = mockPrestador();

        Negocio n = new Negocio();
        n.setId(10);
        n.setCriador(dono);

        when(negRepo.findById(10)).thenReturn(Optional.of(n));

        assertThrows(SecurityException.class, () ->
                service.relatorioNegocio(10, YearMonth.now()));
    }
}
