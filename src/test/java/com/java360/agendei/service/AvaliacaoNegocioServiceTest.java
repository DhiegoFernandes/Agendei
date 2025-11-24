package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.AvaliacaoNegocioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.AvaliacaoNegocioRepository;
import com.java360.agendei.infrastructure.dto.negocio.AvaliacaoNegocioDTO;
import com.java360.agendei.infrastructure.dto.negocio.CreateAvaliacaoNegocioDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoNegocioServiceTest {

    @Mock
    private AvaliacaoNegocioRepository avaliacaoRepo;

    @Mock
    private AgendamentoRepository agendamentoRepo;

    @InjectMocks
    private AvaliacaoNegocioService service;

    private MockedStatic<UsuarioAutenticado> mockAuth;
    private MockedStatic<PermissaoUtils> mockPerm;

    private Cliente cliente;
    private Negocio negocio;
    private Prestador prestador;
    private Agendamento agendamento;

    @BeforeEach
    void setup() {
        cliente = new Cliente();
        cliente.setId(1);
        cliente.setNome("Cliente A");

        negocio = new Negocio();
        negocio.setId(10);
        negocio.setNome("Meu Negócio");

        prestador = new Prestador();
        prestador.setId(99);
        prestador.setNegocio(negocio);

        agendamento = new Agendamento();
        agendamento.setId(100);
        agendamento.setCliente(cliente);
        agendamento.setPrestador(prestador);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);

        mockAuth = Mockito.mockStatic(UsuarioAutenticado.class);
        mockPerm = Mockito.mockStatic(PermissaoUtils.class);

        mockAuth.when(UsuarioAutenticado::get).thenReturn(cliente);
        mockPerm.when(() -> PermissaoUtils.validarPermissao(any(), any())).thenAnswer(i -> null);
    }

    @AfterEach
    void tearDown() {
        mockAuth.close();
        mockPerm.close();
    }


    @Test
    void criarAvaliacao_sucesso_novaAvaliacao() {
        CreateAvaliacaoNegocioDTO dto = new CreateAvaliacaoNegocioDTO();
        dto.setAgendamentoId(100);
        dto.setNota(5);
        dto.setComentario("Ótimo serviço!");

        when(agendamentoRepo.findById(100)).thenReturn(Optional.of(agendamento));
        when(avaliacaoRepo.findByNegocioIdAndClienteId(10, 1)).thenReturn(Optional.empty());

        AvaliacaoNegocio salva = AvaliacaoNegocio.builder()
                .id(300)
                .cliente(cliente)
                .agendamento(agendamento)
                .negocio(negocio)
                .nota(5)
                .comentario("Ótimo serviço!")
                .dataAvaliacao(LocalDateTime.now())
                .build();

        when(avaliacaoRepo.save(any())).thenReturn(salva);
        when(avaliacaoRepo.findByNegocioId(10)).thenReturn(List.of(salva));

        AvaliacaoNegocioDTO result = service.criarAvaliacao(dto);

        assertEquals(300, result.getId());
        assertEquals(5, result.getNota());
        assertEquals("Ótimo serviço!", result.getComentario());
    }

    @Test
    void criarAvaliacao_sucesso_atualizaExistente() {
        CreateAvaliacaoNegocioDTO dto = new CreateAvaliacaoNegocioDTO();
        dto.setAgendamentoId(100);
        dto.setNota(4);
        dto.setComentario("Bom");

        AvaliacaoNegocio existente = AvaliacaoNegocio.builder()
                .id(200)
                .cliente(cliente)
                .agendamento(agendamento)
                .negocio(negocio)
                .nota(3)
                .comentario("Ok")
                .dataAvaliacao(LocalDateTime.now().minusDays(1))
                .build();

        when(agendamentoRepo.findById(100)).thenReturn(Optional.of(agendamento));
        when(avaliacaoRepo.findByNegocioIdAndClienteId(10, 1)).thenReturn(Optional.of(existente));
        when(avaliacaoRepo.save(any())).thenReturn(existente);
        when(avaliacaoRepo.findByNegocioId(10)).thenReturn(List.of(existente));

        AvaliacaoNegocioDTO result = service.criarAvaliacao(dto);

        assertEquals(200, result.getId());
        assertEquals(4, result.getNota());
        assertEquals("Bom", result.getComentario());
    }

    @Test
    void criarAvaliacao_erro_agendamentoNaoEncontrado() {
        CreateAvaliacaoNegocioDTO dto = new CreateAvaliacaoNegocioDTO();
        dto.setAgendamentoId(999);
        dto.setNota(5);

        when(agendamentoRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.criarAvaliacao(dto));
    }

    @Test
    void criarAvaliacao_erro_agendamentoNaoPertenceAoCliente() {
        Cliente outro = new Cliente();
        outro.setId(999);

        agendamento.setCliente(outro);

        CreateAvaliacaoNegocioDTO dto = new CreateAvaliacaoNegocioDTO();
        dto.setAgendamentoId(100);
        dto.setNota(5);

        when(agendamentoRepo.findById(100)).thenReturn(Optional.of(agendamento));

        assertThrows(SecurityException.class,
                () -> service.criarAvaliacao(dto));
    }

    @Test
    void criarAvaliacao_erro_agendamentoNaoConcluido() {
        agendamento.setStatus(StatusAgendamento.PENDENTE);

        CreateAvaliacaoNegocioDTO dto = new CreateAvaliacaoNegocioDTO();
        dto.setAgendamentoId(100);
        dto.setNota(5);

        when(agendamentoRepo.findById(100)).thenReturn(Optional.of(agendamento));

        assertThrows(IllegalArgumentException.class,
                () -> service.criarAvaliacao(dto));
    }

    @Test
    void criarAvaliacao_erro_notaInvalida() {
        CreateAvaliacaoNegocioDTO dto = new CreateAvaliacaoNegocioDTO();
        dto.setAgendamentoId(100);
        dto.setNota(10); // inválido

        when(agendamentoRepo.findById(100)).thenReturn(Optional.of(agendamento));

        assertThrows(IllegalArgumentException.class,
                () -> service.criarAvaliacao(dto));
    }


    @Test
    void listarAvaliacoesNegocio_sucesso() {
        AvaliacaoNegocio a = AvaliacaoNegocio.builder()
                .id(1)
                .negocio(negocio)
                .cliente(cliente)
                .nota(5)
                .comentario("Perfeito")
                .dataAvaliacao(LocalDateTime.now())
                .build();

        when(avaliacaoRepo.findByNegocioId(10))
                .thenReturn(List.of(a));

        List<AvaliacaoNegocioDTO> lista = service.listarAvaliacoesNegocio(10);

        assertEquals(1, lista.size());
        assertEquals(5, lista.get(0).getNota());
    }

    private void invokeAtualizarMedia(Negocio n) {
        try {
            Method m = AvaliacaoNegocioService.class
                    .getDeclaredMethod("atualizarMediaNegocio", Negocio.class);
            m.setAccessible(true);
            m.invoke(service, n);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void atualizarMediaNegocio_sucesso() {
        AvaliacaoNegocio av1 = AvaliacaoNegocio.builder().nota(5).negocio(negocio).build();
        AvaliacaoNegocio av2 = AvaliacaoNegocio.builder().nota(3).negocio(negocio).build();

        when(avaliacaoRepo.findByNegocioId(10))
                .thenReturn(List.of(av1, av2));

        invokeAtualizarMedia(negocio);

        assertEquals(4.0, negocio.getNotaMedia());
    }
}
