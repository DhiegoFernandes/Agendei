package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.ServicoService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.dto.*;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DisponibilidadeRepository disponibilidadeRepository;
    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private NegocioRepository negocioRepository;

    @InjectMocks
    private ServicoService servicoService;

    private MockedStatic<UsuarioAutenticado> mockUsuarioAuth;
    private MockedStatic<PermissaoUtils> mockPermissao;

    private Prestador prestador;
    private Negocio negocio;

    @BeforeEach
    void setup() {
        negocio = new Negocio();
        negocio.setId(1);

        prestador = new Prestador();
        prestador.setId(10);
        prestador.setNome("Prestador X");
        prestador.setNegocio(negocio);

        mockUsuarioAuth = Mockito.mockStatic(UsuarioAutenticado.class);
        mockPermissao = Mockito.mockStatic(PermissaoUtils.class);

        mockUsuarioAuth.when(UsuarioAutenticado::get).thenReturn(prestador);
        mockPermissao.when(() -> PermissaoUtils.validarPermissao(any(), any(), any())).thenAnswer(inv -> null);
        mockPermissao.when(() -> PermissaoUtils.isAdmin(any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        mockUsuarioAuth.close();
        mockPermissao.close();
    }
    @Test
    void cadastrarServico_sucesso() {
        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("Corte");
        dto.setDescricao("Corte de cabelo");
        dto.setValor(80);
        dto.setDuracaoMinutos(30);
        dto.setAtivo(true);

        when(servicoRepository.existsByTituloAndNegocioId("Corte", 1)).thenReturn(false);

        Servico salvo = Servico.builder()
                .id(99)
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .valor(dto.getValor())
                .duracaoMinutos(dto.getDuracaoMinutos())
                .ativo(true)
                .prestador(prestador)
                .negocio(negocio)
                .build();

        when(servicoRepository.save(any())).thenReturn(salvo);

        Servico result = servicoService.cadastrarServico(dto);

        assertEquals(99, result.getId());
        verify(servicoRepository).save(any());
    }

    @Test
    void cadastrarServico_duracaoInvalida() {
        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("X");
        dto.setDescricao("Y");
        dto.setValor(100);
        dto.setDuracaoMinutos(600);
        dto.setAtivo(true);

        assertThrows(IllegalArgumentException.class, () ->
                servicoService.cadastrarServico(dto));
    }

    @Test
    void cadastrarServico_valorInvalido() {
        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("X");
        dto.setDescricao("Y");
        dto.setValor(99999);
        dto.setDuracaoMinutos(30);
        dto.setAtivo(true);

        assertThrows(IllegalArgumentException.class, () ->
                servicoService.cadastrarServico(dto));
    }

    @Test
    void cadastrarServico_tituloDuplicado() {
        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("Corte");
        dto.setDescricao("Y");
        dto.setValor(50);
        dto.setDuracaoMinutos(30);
        dto.setAtivo(true);

        when(servicoRepository.existsByTituloAndNegocioId("Corte", 1))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                servicoService.cadastrarServico(dto));
    }

    @Test
    void listarHorarios_dataPassada_retornaVazio() {
        Servico servico = Servico.builder()
                .id(1)
                .ativo(true)
                .duracaoMinutos(30)
                .prestador(prestador)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1))
                .thenReturn(Optional.of(servico));

        Disponibilidade disp = new Disponibilidade();
        disp.setDiaSemana(DiaSemanaDisponivel.values()[LocalDate.now().minusDays(1).getDayOfWeek().getValue() - 1]);
        disp.setAtivo(true);
        disp.setHoraInicio(LocalTime.of(8, 0));
        disp.setHoraFim(LocalTime.of(12, 0));

        when(disponibilidadeRepository.findByPrestadorId(prestador.getId()))
                .thenReturn(List.of(disp));

        LocalDate ontem = LocalDate.now().minusDays(1);

        HorariosDisponiveisDTO dto =
                servicoService.listarHorariosPorServicoEData(1, ontem);

        assertTrue(dto.getDiasDisponiveis().isEmpty());
    }


    @Test
    void listarHorarios_servicoDesativado_erro() {
        Servico servico = Servico.builder()
                .id(1)
                .ativo(false)
                .prestador(prestador)
                .duracaoMinutos(30)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));

        assertThrows(IllegalArgumentException.class, () ->
                servicoService.listarHorariosPorServicoEData(1, LocalDate.now()));
    }

    @Test
    void listarHorarios_semDisponibilidade_erro() {
        Servico servico = Servico.builder()
                .id(1)
                .ativo(true)
                .prestador(prestador)
                .duracaoMinutos(30)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));
        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                servicoService.listarHorariosPorServicoEData(1, LocalDate.now()));
    }

    @Test
    void listarHorarios_sucesso_semConflitos() {
        Servico servico = Servico.builder()
                .id(1)
                .ativo(true)
                .prestador(prestador)
                .duracaoMinutos(60)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));

        Disponibilidade disp = new Disponibilidade();
        disp.setDiaSemana(DiaSemanaDisponivel.SEGUNDA);
        disp.setAtivo(true);
        disp.setHoraInicio(LocalTime.of(8, 0));
        disp.setHoraFim(LocalTime.of(12, 0));

        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of(disp));

        when(agendamentoRepository.findByPrestadorIdAndStatusAndDataHoraBetween(any(), any(), any(), any()))
                .thenReturn(List.of());

        LocalDate proximaSegunda = LocalDate.now().plusDays(1);

        HorariosDisponiveisDTO dto =
                servicoService.listarHorariosPorServicoEData(1, proximaSegunda);

        assertFalse(dto.getDiasDisponiveis().isEmpty());
        assertEquals("08:00", dto.getDiasDisponiveis().get(0).getHorarios().get(0));
    }

    @Test
    void listarHorarios_conflitoComAgendamento_naoAdicionaHorario() {
        Servico servico = Servico.builder()
                .id(1)
                .ativo(true)
                .duracaoMinutos(60)
                .prestador(prestador)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));

        Disponibilidade disp = new Disponibilidade();
        disp.setDiaSemana(DiaSemanaDisponivel.SEGUNDA);
        disp.setHoraInicio(LocalTime.of(8, 0));
        disp.setHoraFim(LocalTime.of(12, 0));
        disp.setAtivo(true);

        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of(disp));

        Agendamento ag = new Agendamento();
        ag.setDataHora(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(8, 0)));
        ag.setServico(servico);

        when(agendamentoRepository.findByPrestadorIdAndStatusAndDataHoraBetween(any(), any(), any(), any()))
                .thenReturn(List.of(ag));

        HorariosDisponiveisDTO dto =
                servicoService.listarHorariosPorServicoEData(1, LocalDate.now().plusDays(1));

        List<String> horarios = dto.getDiasDisponiveis().get(0).getHorarios();

        assertFalse(horarios.contains("08:00"));
        assertTrue(horarios.contains("09:00")); // horários válidos continuam
    }


    @Test
    void atualizarServico_sucesso() {
        Servico servico = Servico.builder()
                .id(1)
                .titulo("A")
                .descricao("B")
                .valor(100)
                .duracaoMinutos(30)
                .ativo(true)
                .prestador(prestador)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));
        when(servicoRepository.existsByTituloAndPrestadorIdAndIdNot(any(), any(), any()))
                .thenReturn(false);

        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("Novo");
        dto.setDescricao("Desc");
        dto.setValor(120);
        dto.setDuracaoMinutos(40);
        dto.setAtivo(true);

        Servico atualizado = servicoService.atualizarServico(1, dto);

        assertEquals("Novo", atualizado.getTitulo());
        assertEquals(120, atualizado.getValor());
    }

    @Test
    void atualizarServico_tituloDuplicado() {
        Servico servico = Servico.builder()
                .id(1)
                .prestador(prestador)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));
        when(servicoRepository.existsByTituloAndPrestadorIdAndIdNot(any(), any(), any()))
                .thenReturn(true);

        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("X");
        dto.setDescricao("Y");
        dto.setValor(100);
        dto.setDuracaoMinutos(30);
        dto.setAtivo(true);

        assertThrows(IllegalArgumentException.class, () ->
                servicoService.atualizarServico(1, dto));
    }

    @Test
    void buscarServicos_sucesso() {
        Servico s = new Servico();
        s.setId(1);
        s.setTitulo("Corte");
        s.setDescricao("Cabelo");
        s.setValor(50);
        s.setDuracaoMinutos(30);
        s.setAtivo(true);
        s.setPrestador(prestador);
        s.setNegocio(negocio);

        when(servicoRepository.buscarServicos(any(), any(), any()))
                .thenReturn(List.of(s));

        List<ServicoDTO> result =
                servicoService.buscarServicos("Corte", "Prestador X", DiaSemanaDisponivel.SEGUNDA);

        assertEquals(1, result.size());
        assertEquals("Corte", result.get(0).getTitulo());
    }

    @Test
    void listarServicosAtivos_sucesso() {
        when(servicoRepository.findAllByAtivoTrue())
                .thenReturn(List.of(new Servico()));

        assertEquals(1, servicoService.listarServicosAtivos().size());
    }

    @Test
    void listarServicosPorNegocio_sucesso() {
        Servico s = new Servico();
        s.setId(1);
        s.setTitulo("Corte");
        s.setDescricao("Desc");
        s.setValor(100);
        s.setDuracaoMinutos(30);
        s.setAtivo(true);
        s.setPrestador(prestador);
        s.setNegocio(negocio);

        when(servicoRepository.findByNegocio_IdAndAtivoTrue(1))
                .thenReturn(List.of(s));

        List<ServicoDTO> result = servicoService.listarServicosPorNegocio(1);

        assertEquals(1, result.size());
    }

    @Test
    void listarTodosServicosPorNegocio_sucesso() {
        Negocio neg = new Negocio();
        neg.setId(1);

        Prestador p = new Prestador();
        p.setId(10);
        p.setNome("Prestador");
        p.setFotoPerfil("foto".getBytes()); // pega bytes da foto
        p.setNegocio(neg);

        Servico servico = new Servico();
        servico.setId(1);
        servico.setTitulo("Corte");
        servico.setDescricao("Desc");
        servico.setValor(50);
        servico.setDuracaoMinutos(30);
        servico.setPrestador(p);
        servico.setNegocio(neg);
        servico.setAtivo(true);

        when(negocioRepository.findById(1))
                .thenReturn(Optional.of(neg));

        when(servicoRepository.findByNegocio_Id(1))
                .thenReturn(List.of(servico));

        List<ServicoDTO> result =
                servicoService.listarTodosServicosPorNegocio(1);

        assertEquals(1, result.size());
        assertEquals("Corte", result.get(0).getTitulo());
    }

    // teste de sobreposiçao de horario
    @Test
    void overlaps_quandoNaoSobrepoe_retornaFalse() {
        LocalTime a1 = LocalTime.of(8, 0);
        LocalTime a2 = LocalTime.of(9, 0);

        LocalTime b1 = LocalTime.of(9, 0);
        LocalTime b2 = LocalTime.of(10, 0);

        assertFalse(invokeOverlaps(a1, a2, b1, b2));
    }

    @Test
    void overlaps_quandoSobrepoe_retornaTrue() {
        LocalTime a1 = LocalTime.of(8, 0);
        LocalTime a2 = LocalTime.of(10, 0);

        LocalTime b1 = LocalTime.of(9, 0);
        LocalTime b2 = LocalTime.of(11, 0);

        assertTrue(invokeOverlaps(a1, a2, b1, b2));
    }

    // Helper usando reflexão para testar método private
    private boolean invokeOverlaps(LocalTime i1, LocalTime f1, LocalTime i2, LocalTime f2) {
        try {
            Method m = ServicoService.class.getDeclaredMethod("overlaps",
                    LocalTime.class, LocalTime.class, LocalTime.class, LocalTime.class);
            m.setAccessible(true);
            return (boolean) m.invoke(servicoService, i1, f1, i2, f2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void traduzirDiaDaSemana_deveRetornarCorreto() {
        assertEquals("SEGUNDA", invokeTraduzir(DayOfWeek.MONDAY));
        assertEquals("DOMINGO", invokeTraduzir(DayOfWeek.SUNDAY));
        assertEquals("SABADO", invokeTraduzir(DayOfWeek.SATURDAY));
    }

    private String invokeTraduzir(DayOfWeek dia) {
        try {
            Method m = ServicoService.class.getDeclaredMethod("traduzirDiaDaSemana", DayOfWeek.class);
            m.setAccessible(true);
            return (String) m.invoke(servicoService, dia);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void listarHorarios_servicoInativo_lancaErro() {
        Servico servico = Servico.builder()
                .ativo(false)
                .prestador(prestador)
                .duracaoMinutos(30)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));

        assertThrows(IllegalArgumentException.class,
                () -> servicoService.listarHorariosPorServicoEData(1, LocalDate.now()));
    }

    @Test
    void listarHorarios_semDisponibilidade_lancaErro() {
        Servico servico = Servico.builder()
                .id(1)
                .ativo(true)
                .duracaoMinutos(30)
                .prestador(prestador)
                .negocio(negocio)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));
        when(disponibilidadeRepository.findByPrestadorId(10)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> servicoService.listarHorariosPorServicoEData(1, LocalDate.now().plusDays(1)));
    }

    @Test
    void atualizarServico_tituloDuplicado_lancaErro() {
        Prestador p = new Prestador();
        p.setId(10);
        mockUsuarioAuth.when(UsuarioAutenticado::get).thenReturn(p);


        Servico servico = Servico.builder()
                .id(1)
                .prestador(p)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));
        when(servicoRepository.existsByTituloAndPrestadorIdAndIdNot("X", 10, 1))
                .thenReturn(true);

        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("X");
        dto.setDescricao("D");
        dto.setValor(10);
        dto.setDuracaoMinutos(30);
        dto.setAtivo(true);

        assertThrows(IllegalArgumentException.class,
                () -> servicoService.atualizarServico(1, dto));
    }


    @Test
    void atualizarServico_semPermissao_lancaErro() {
        Prestador dono = new Prestador();
        dono.setId(10);

        Prestador p = new Prestador();
        p.setNegocio(new Negocio());
        mockUsuarioAuth.when(UsuarioAutenticado::get).thenReturn(p);

        Servico servico = Servico.builder()
                .id(1)
                .prestador(dono)
                .build();

        when(servicoRepository.findById(1)).thenReturn(Optional.of(servico));

        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("A");
        dto.setDescricao("B");
        dto.setValor(10);
        dto.setDuracaoMinutos(30);
        dto.setAtivo(true);

        assertThrows(SecurityException.class,
                () -> servicoService.atualizarServico(1, dto));
    }

    @Test
    void listarServicosAtivos_retornaLista() {
        when(servicoRepository.findAllByAtivoTrue())
                .thenReturn(List.of(new Servico(), new Servico()));

        List<Servico> lista = servicoService.listarServicosAtivos();
        assertEquals(2, lista.size());
    }

    @Test
    void cadastrarServico_duracaoMaiorQueLimite_lancaErro() {
        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("Serviço");
        dto.setDescricao("Desc");
        dto.setValor(50);
        dto.setDuracaoMinutos(999);
        dto.setAtivo(true);

        Prestador p = new Prestador();
        p.setNegocio(new Negocio());
        mockUsuarioAuth.when(UsuarioAutenticado::get).thenReturn(p);

        assertThrows(IllegalArgumentException.class,
                () -> servicoService.cadastrarServico(dto));
    }

    @Test
    void cadastrarServico_valorMaiorQueLimite_lancaErro() {
        SaveServicoDTO dto = new SaveServicoDTO();
        dto.setTitulo("Serviço");
        dto.setDescricao("Desc");
        dto.setValor(9999);
        dto.setDuracaoMinutos(30);
        dto.setAtivo(true);

        Prestador p = new Prestador();
        p.setNegocio(new Negocio());
        mockUsuarioAuth.when(UsuarioAutenticado::get).thenReturn(p);

        assertThrows(IllegalArgumentException.class,
                () -> servicoService.cadastrarServico(dto));
    }


}
