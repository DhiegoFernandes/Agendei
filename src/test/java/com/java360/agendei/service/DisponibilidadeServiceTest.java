package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.DisponibilidadeService;
import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.DisponibilidadeRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.DisponibilidadeDTO;
import com.java360.agendei.infrastructure.dto.HorarioAlmocoDTO;
import com.java360.agendei.infrastructure.dto.SaveDisponibilidadeDTO;
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
class DisponibilidadeServiceTest {

    @Mock private DisponibilidadeRepository disponibilidadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AgendamentoRepository agendamentoRepository;

    @InjectMocks private DisponibilidadeService service;

    private MockedStatic<UsuarioAutenticado> mockAuth;
    private MockedStatic<PermissaoUtils> mockPerm;

    private Prestador prestador;

    @BeforeEach
    void setup() {
        prestador = new Prestador();
        prestador.setId(10);
        prestador.setNome("Prestador X");

        mockAuth = Mockito.mockStatic(UsuarioAutenticado.class);
        mockPerm = Mockito.mockStatic(PermissaoUtils.class);

        mockAuth.when(UsuarioAutenticado::get).thenReturn(prestador);
        mockPerm.when(() -> PermissaoUtils.validarPermissao(any(), any(), any())).thenAnswer(i -> null);
        mockPerm.when(() -> PermissaoUtils.validarPermissao(any(), any())).thenAnswer(i -> null);
    }

    @AfterEach
    void tearDown() {
        mockAuth.close();
        mockPerm.close();
    }


    @Test
    void prestadorEstaDisponivel_retornaTrue_quandoDentroDaJanela() {
        Disponibilidade d = Disponibilidade.builder()
                .diaSemana(DiaSemanaDisponivel.SEGUNDA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(12, 0))
                .prestador(prestador)
                .ativo(true)
                .build();

        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of(d));

        LocalDateTime inicio = LocalDateTime.of(2025, 1, 6, 9, 0); // segunda
        boolean result = service.prestadorEstaDisponivel(10, inicio, 60);

        assertTrue(result);
    }

    @Test
    void prestadorEstaDisponivel_retornaFalse_quandoForaDaJanela() {
        Disponibilidade d = Disponibilidade.builder()
                .diaSemana(DiaSemanaDisponivel.SEGUNDA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(9, 0))
                .prestador(prestador)
                .ativo(true)
                .build();

        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of(d));

        LocalDateTime inicio = LocalDateTime.of(2025, 1, 6, 8, 30);
        boolean result = service.prestadorEstaDisponivel(10, inicio, 60);

        assertFalse(result);
    }

    @Test
    void prestadorEstaDisponivel_retornaFalse_quandoDiaNaoBate() {
        Disponibilidade d = Disponibilidade.builder()
                .diaSemana(DiaSemanaDisponivel.TERCA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(12, 0))
                .ativo(true)
                .prestador(prestador)
                .build();

        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of(d));

        LocalDateTime segunda = LocalDateTime.of(2025, 1, 6, 9, 0);

        assertFalse(service.prestadorEstaDisponivel(10, segunda, 30));
    }

    @Test
    void prestadorEstaDisponivel_retornaFalse_quandoInativo() {
        Disponibilidade d = Disponibilidade.builder()
                .diaSemana(DiaSemanaDisponivel.SEGUNDA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(12, 0))
                .ativo(false)
                .prestador(prestador)
                .build();

        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of(d));

        LocalDateTime segunda = LocalDateTime.of(2025, 1, 6, 9, 0);

        assertFalse(service.prestadorEstaDisponivel(10, segunda, 30));
    }

    private String traduzir(DayOfWeek d) {
        try {
            Method m = DisponibilidadeService.class.getDeclaredMethod("traduzirDiaDaSemana", DayOfWeek.class);
            m.setAccessible(true);
            return (String) m.invoke(service, d);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void traduzirDiaDaSemana_todosOsDias() {
        assertEquals("DOMINGO", traduzir(DayOfWeek.SUNDAY));
        assertEquals("SEGUNDA", traduzir(DayOfWeek.MONDAY));
        assertEquals("TERCA", traduzir(DayOfWeek.TUESDAY));
        assertEquals("QUARTA", traduzir(DayOfWeek.WEDNESDAY));
        assertEquals("QUINTA", traduzir(DayOfWeek.THURSDAY));
        assertEquals("SEXTA", traduzir(DayOfWeek.FRIDAY));
        assertEquals("SABADO", traduzir(DayOfWeek.SATURDAY));
    }

    @Test
    void cadastrarDisponibilidade_sucesso_novoRegistro() {
        SaveDisponibilidadeDTO dto = new SaveDisponibilidadeDTO();
        dto.setDiaSemana(DiaSemanaDisponivel.SEGUNDA);
        dto.setHoraInicio(LocalTime.of(8, 0));
        dto.setHoraFim(LocalTime.of(12, 0));

        when(disponibilidadeRepository.findByPrestadorIdAndDiaSemana(10, DiaSemanaDisponivel.SEGUNDA))
                .thenReturn(Optional.empty());

        Disponibilidade salvo = Disponibilidade.builder()
                .id(99)
                .diaSemana(DiaSemanaDisponivel.SEGUNDA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(12, 0))
                .prestador(prestador)
                .build();

        when(disponibilidadeRepository.save(any())).thenReturn(salvo);

        DisponibilidadeDTO result = service.cadastrarOuAtualizarDisponibilidade(dto);

        assertEquals(99, result.getId());
        assertEquals("Prestador X", result.getNomePrestador());
    }

    @Test
    void atualizarDisponibilidade_sucesso() {
        SaveDisponibilidadeDTO dto = new SaveDisponibilidadeDTO();
        dto.setDiaSemana(DiaSemanaDisponivel.QUARTA);
        dto.setHoraInicio(LocalTime.of(10, 0));
        dto.setHoraFim(LocalTime.of(18, 0));

        Disponibilidade existente = Disponibilidade.builder()
                .id(5)
                .diaSemana(DiaSemanaDisponivel.QUARTA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(12, 0))
                .prestador(prestador)
                .build();

        when(disponibilidadeRepository.findByPrestadorIdAndDiaSemana(10, DiaSemanaDisponivel.QUARTA))
                .thenReturn(Optional.of(existente));

        when(disponibilidadeRepository.save(any())).thenReturn(existente);

        DisponibilidadeDTO result = service.cadastrarOuAtualizarDisponibilidade(dto);

        assertEquals(5, result.getId());
        assertEquals(LocalTime.of(10, 0), result.getHoraInicio());
    }

    @Test
    void cadastrarDisponibilidade_erro_horarioInvalido() {
        SaveDisponibilidadeDTO dto = new SaveDisponibilidadeDTO();
        dto.setDiaSemana(DiaSemanaDisponivel.SEXTA);
        dto.setHoraInicio(LocalTime.of(12, 0));
        dto.setHoraFim(LocalTime.of(10, 0));

        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrarOuAtualizarDisponibilidade(dto));
    }

    @Test
    void cadastrarDisponibilidade_erro_fimDepoisDe2359() {
        SaveDisponibilidadeDTO dto = new SaveDisponibilidadeDTO();
        dto.setDiaSemana(DiaSemanaDisponivel.DOMINGO);
        dto.setHoraInicio(LocalTime.of(10, 0));
        dto.setHoraFim(LocalTime.of(23, 59, 59)); // > 23:59

        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrarOuAtualizarDisponibilidade(dto));
    }

    @Test
    void alterarStatusDia_sucesso() {
        Disponibilidade d = Disponibilidade.builder()
                .id(1)
                .diaSemana(DiaSemanaDisponivel.SEGUNDA)
                .prestador(prestador)
                .ativo(true)
                .build();

        when(disponibilidadeRepository.findByPrestadorIdAndDiaSemana(10, DiaSemanaDisponivel.SEGUNDA))
                .thenReturn(Optional.of(d));

        Disponibilidade result = service.alterarStatusDia(DiaSemanaDisponivel.SEGUNDA, false);

        assertFalse(result.isAtivo());
    }

    @Test
    void alterarStatusDia_erro_diaNaoEncontrado() {
        when(disponibilidadeRepository.findByPrestadorIdAndDiaSemana(10, DiaSemanaDisponivel.SABADO))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.alterarStatusDia(DiaSemanaDisponivel.SABADO, true));
    }

    @Test
    void definirHorarioAlmoco_sucesso() {
        // quando salvar o prestador
        when(usuarioRepository.save(any())).thenReturn(prestador);

        // evitar NullPointer durante o cancelamento de agendamentos
        when(agendamentoRepository.findByPrestadorId(10))
                .thenReturn(Collections.emptyList());

        service.definirHorarioAlmoco(LocalTime.of(12, 0));

        assertEquals(LocalTime.of(12, 0), prestador.getHoraInicioAlmoco());
        assertEquals(LocalTime.of(13, 0), prestador.getHoraFimAlmoco());
    }


    @Test
    void definirHorarioAlmoco_erro_horarioInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.definirHorarioAlmoco(LocalTime.of(3, 0)));
    }

    @Test
    void buscarHorarioAlmoco_sucesso() {
        prestador.setHoraInicioAlmoco(LocalTime.of(11, 0));
        prestador.setHoraFimAlmoco(LocalTime.of(12, 0));

        HorarioAlmocoDTO dto = service.buscarHorarioAlmoco();

        assertEquals(LocalTime.of(11, 0), dto.getHoraInicioAlmoco());
    }

    @Test
    void buscarHorarioAlmoco_erro_naoDefinido() {
        prestador.setHoraInicioAlmoco(null);
        prestador.setHoraFimAlmoco(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.buscarHorarioAlmoco());
    }

    @Test
    void listarPorPrestadorAutenticado_sucesso() {
        Disponibilidade d = new Disponibilidade();
        d.setPrestador(prestador);

        when(disponibilidadeRepository.findByPrestadorId(10))
                .thenReturn(List.of(d));

        List<Disponibilidade> lista = service.listarPorPrestadorAutenticado();

        assertEquals(1, lista.size());
    }

}
