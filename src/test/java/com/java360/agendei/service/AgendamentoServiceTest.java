package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.AgendamentoService;
import com.java360.agendei.domain.applicationservice.DisponibilidadeService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.ClienteBloqueadoRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.ClienteResumoDTO;
import com.java360.agendei.infrastructure.dto.CreateAgendamentoDTO;
import com.java360.agendei.infrastructure.email.EmailService;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DisponibilidadeService disponibilidadeService;
    @Mock
    private ClienteBloqueadoRepository clienteBloqueadoRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private MockedStatic<UsuarioAutenticado> usuarioAutenticadoMock;
    private MockedStatic<PermissaoUtils> permissaoUtilsMock;

    @BeforeEach
    void setUp() {
        usuarioAutenticadoMock = mockStatic(UsuarioAutenticado.class);
        permissaoUtilsMock = mockStatic(PermissaoUtils.class);
    }

    @AfterEach
    void tearDown() {
        usuarioAutenticadoMock.close();
        permissaoUtilsMock.close();
    }

    private Cliente criarCliente(Integer id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome("Cliente " + id);
        c.setEmail("cliente" + id + "@teste.com");
        c.setTelefone("1199999000" + id);
        c.setSenha("123");
        c.setPerfil(PerfilUsuario.CLIENTE);
        c.setCep("00000-000");
        c.setEndereco("Rua X");
        c.setNumero("10");
        return c;
    }

    private Prestador criarPrestador(Integer id, Negocio negocio) {
        Prestador p = new Prestador();
        p.setId(id);
        p.setNome("Prestador " + id);
        p.setEmail("prestador" + id + "@teste.com");
        p.setTelefone("1188888000" + id);
        p.setSenha("123");
        p.setPerfil(PerfilUsuario.PRESTADOR);
        p.setNegocio(negocio);
        return p;
    }

    private Negocio criarNegocio(Integer id, boolean ativo) {
        Negocio n = new Negocio();
        n.setId(id);
        n.setNome("Negocio " + id);
        n.setEndereco("Rua Y");
        n.setNumero("123");
        n.setCep("00000-000");
        n.setAtivo(ativo);
        return n;
    }

    private Servico criarServico(Integer id, Prestador prestador, Negocio negocio, boolean ativo, int duracaoMin) {
        return Servico.builder()
                .id(id)
                .titulo("Corte")
                .descricao("Corte de cabelo")
                .valor(50.0)
                .duracaoMinutos(duracaoMin)
                .ativo(ativo)
                .prestador(prestador)
                .negocio(negocio)
                .build();
    }


    @Test
    void criarAgendamento_deveCriarComSucesso() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(cliente)).thenReturn(false);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 60);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        LocalDateTime dataHora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);
        when(dto.getServicoId()).thenReturn(servico.getId());
        when(dto.getDataHora()).thenReturn(dataHora);

        when(agendamentoRepository.countByClienteIdAndStatus(cliente.getId(), StatusAgendamento.PENDENTE))
                .thenReturn(0L);
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), cliente.getId()))
                .thenReturn(Optional.empty());
        when(disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), dataHora, servico.getDuracaoMinutos()))
                .thenReturn(true);
        when(agendamentoRepository.findByPrestadorId(prestador.getId()))
                .thenReturn(List.of());
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> {
            Agendamento a = inv.getArgument(0);
            a.setId(99);
            return a;
        });

        Agendamento salvo = agendamentoService.criarAgendamento(dto);

        assertNotNull(salvo);
        assertEquals(99, salvo.getId());
        assertEquals(cliente, salvo.getCliente());
        assertEquals(servico, salvo.getServico());
        assertEquals(StatusAgendamento.PENDENTE, salvo.getStatus());
        verify(emailService).enviarConfirmacaoAgendamento(salvo);
    }

    @Test
    void criarAgendamento_deveLancarErroQuandoClienteTemQuatroPendentes() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        when(agendamentoRepository.countByClienteIdAndStatus(cliente.getId(), StatusAgendamento.PENDENTE))
                .thenReturn(4L);


        when(agendamentoRepository.countByClienteIdAndStatus(cliente.getId(), StatusAgendamento.PENDENTE))
                .thenReturn(4L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("limite máximo de 4 agendamentos"));
    }

    @Test
    void criarAgendamento_deveLancarErroQuandoServicoIdNulo() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        when(dto.getServicoId()).thenReturn(null);
        lenient().when(dto.getDataHora()).thenReturn(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1));

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("ID do serviço é obrigatório"));
    }

    @Test
    void criarAgendamento_naoPermiteDataNoPassado() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        when(dto.getServicoId()).thenReturn(1);
        when(dto.getDataHora()).thenReturn(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusDays(1));

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("passado"));
    }

    @Test
    void criarAgendamento_naoPermiteServicoInativo() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, false, 60);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        when(dto.getServicoId()).thenReturn(servico.getId());
        when(dto.getDataHora()).thenReturn(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1));

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("serviço inativo"));
    }

    @Test
    void criarAgendamento_naoPermiteNegocioInativo() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, false);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 60);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        when(dto.getServicoId()).thenReturn(servico.getId());
        when(dto.getDataHora()).thenReturn(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1));

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("negócio inativo"));
    }

    @Test
    void criarAgendamento_naoPermiteClienteBloqueado() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 60);

        ClienteBloqueado bloqueio = ClienteBloqueado.builder()
                .id(5)
                .negocio(negocio)
                .cliente(cliente)
                .ativo(true)
                .build();

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        when(dto.getServicoId()).thenReturn(servico.getId());
        when(dto.getDataHora()).thenReturn(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1));

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), cliente.getId()))
                .thenReturn(Optional.of(bloqueio));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("bloqueado"));
    }

    @Test
    void criarAgendamento_naoPermiteNoHorarioDeAlmoco() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        prestador.setHoraInicioAlmoco(LocalTime.of(12, 0));
        prestador.setHoraFimAlmoco(LocalTime.of(13, 0));
        Servico servico = criarServico(3, prestador, negocio, true, 60);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        LocalDateTime dataHora = LocalDateTime.of(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1), LocalTime.of(12, 30));
        when(dto.getServicoId()).thenReturn(servico.getId());
        when(dto.getDataHora()).thenReturn(dataHora);

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), cliente.getId()))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("horário de almoço"));
    }

    @Test
    void criarAgendamento_naoPermiteQuandoPrestadorIndisponivel() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 60);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        LocalDateTime dataHora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);
        when(dto.getServicoId()).thenReturn(servico.getId());
        when(dto.getDataHora()).thenReturn(dataHora);

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), cliente.getId()))
                .thenReturn(Optional.empty());
        when(disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), dataHora, servico.getDuracaoMinutos()))
                .thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("não está disponível"));
    }

    @Test
    void criarAgendamento_naoPermiteConflitoComOutrosAgendamentos() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 60);

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        LocalDateTime dataHora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1).withHour(10).withMinute(0);
        when(dto.getServicoId()).thenReturn(servico.getId());
        when(dto.getDataHora()).thenReturn(dataHora);

        when(agendamentoRepository.countByClienteIdAndStatus(anyInt(), any()))
                .thenReturn(0L);
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), cliente.getId()))
                .thenReturn(Optional.empty());
        when(disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), dataHora, servico.getDuracaoMinutos()))
                .thenReturn(true);

        // agendamento existente 10:00 - 11:00 pendente
        Agendamento existente = Agendamento.builder()
                .id(50)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(dataHora)
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findByPrestadorId(prestador.getId()))
                .thenReturn(List.of(existente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.criarAgendamento(dto));

        assertTrue(ex.getMessage().contains("Horário indisponível"));
    }


    @Test
    void atualizarAgendamento_deveAtualizarComSucesso() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(cliente)).thenReturn(false);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servicoAntigo = criarServico(3, prestador, negocio, true, 30);
        Servico servicoNovo = criarServico(4, prestador, negocio, true, 60);

        LocalDateTime dataAntiga = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1).withHour(9);
        Agendamento agendamento = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servicoAntigo)
                .dataHora(dataAntiga)
                .status(StatusAgendamento.PENDENTE)
                .build();

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        LocalDateTime novaData = dataAntiga.plusHours(2);
        when(dto.getServicoId()).thenReturn(servicoNovo.getId());
        when(dto.getDataHora()).thenReturn(novaData);

        when(agendamentoRepository.findById(agendamento.getId()))
                .thenReturn(Optional.of(agendamento));
        when(servicoRepository.findById(servicoNovo.getId()))
                .thenReturn(Optional.of(servicoNovo));
        when(disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), novaData, servicoNovo.getDuracaoMinutos()))
                .thenReturn(true);
        when(agendamentoRepository.findByPrestadorId(prestador.getId()))
                .thenReturn(List.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento atualizado = agendamentoService.atualizarAgendamento(agendamento.getId(), dto);

        assertEquals(servicoNovo, atualizado.getServico());
        assertEquals(novaData, atualizado.getDataHora());
        assertEquals(StatusAgendamento.PENDENTE, atualizado.getStatus());
    }

    @Test
    void atualizarAgendamento_naoPermiteAlterarCancelado() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento agendamento = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))
                .status(StatusAgendamento.CANCELADO)
                .build();

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        lenient().when(dto.getDataHora()).thenReturn(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(2));

        when(agendamentoRepository.findById(agendamento.getId()))
                .thenReturn(Optional.of(agendamento));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.atualizarAgendamento(agendamento.getId(), dto));

        assertTrue(ex.getMessage().contains("agendamento cancelado"));
    }

    @Test
    void atualizarAgendamento_verificaPermissao() {
        // usuário diferente de cliente/prestador/admin
        Usuario outro = new Usuario();
        outro.setId(99);
        outro.setNome("Intruso");
        outro.setPerfil(PerfilUsuario.CLIENTE);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(outro);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(outro)).thenReturn(false);

        Cliente cliente = criarCliente(1);
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento agendamento = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))
                .status(StatusAgendamento.PENDENTE)
                .build();

        CreateAgendamentoDTO dto = mock(CreateAgendamentoDTO.class);
        lenient().when(dto.getDataHora()).thenReturn(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(2));

        when(agendamentoRepository.findById(agendamento.getId()))
                .thenReturn(Optional.of(agendamento));

        assertThrows(SecurityException.class,
                () -> agendamentoService.atualizarAgendamento(agendamento.getId(), dto));
    }

    // ========= concluirAgendamento / cancelarAgendamento =========

    @Test
    void concluirAgendamento_deveConcluirQuandoPrestador() {
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(prestador)).thenReturn(false);

        Cliente cliente = criarCliente(1);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento ag = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findById(ag.getId())).thenReturn(Optional.of(ag));

        agendamentoService.concluirAgendamento(ag.getId());

        assertEquals(StatusAgendamento.CONCLUIDO, ag.getStatus());
    }

    @Test
    void concluirAgendamento_lancaErroQuandoSemPermissao() {
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);

        Usuario outro = new Usuario();
        outro.setId(99);
        outro.setPerfil(PerfilUsuario.CLIENTE);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(outro);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(outro)).thenReturn(false);

        Cliente cliente = criarCliente(1);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento ag = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findById(ag.getId())).thenReturn(Optional.of(ag));

        assertThrows(SecurityException.class,
                () -> agendamentoService.concluirAgendamento(ag.getId()));
    }

    @Test
    void cancelarAgendamento_deveCancelarQuandoCliente() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(cliente)).thenReturn(false);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento ag = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findById(ag.getId())).thenReturn(Optional.of(ag));

        agendamentoService.cancelarAgendamento(ag.getId());

        assertEquals(StatusAgendamento.CANCELADO, ag.getStatus());
    }

    @Test
    void cancelarAgendamento_naoPermiteQuandoSemPermissao() {
        Usuario outro = new Usuario();
        outro.setId(99);
        outro.setPerfil(PerfilUsuario.CLIENTE);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(outro);
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(outro)).thenReturn(false);

        Cliente cliente = criarCliente(1);
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento ag = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findById(ag.getId())).thenReturn(Optional.of(ag));

        assertThrows(SecurityException.class,
                () -> agendamentoService.cancelarAgendamento(ag.getId()));
    }

    @Test
    void cancelarAgendamento_naoPermiteSeNaoPendente() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento ag = Agendamento.builder()
                .id(100)
                .cliente(cliente)
                .prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))
                .status(StatusAgendamento.CONCLUIDO)
                .build();

        when(agendamentoRepository.findById(ag.getId())).thenReturn(Optional.of(ag));
        permissaoUtilsMock.when(() -> PermissaoUtils.isAdmin(cliente)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agendamentoService.cancelarAgendamento(ag.getId()));

        assertTrue(ex.getMessage().contains("pendentes"));
    }


    @Test
    void listarAgendamentosCliente_deveRetornarLista() {
        Cliente cliente = criarCliente(1);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(cliente);

        when(agendamentoRepository.findByClienteId(cliente.getId()))
                .thenReturn(List.of(new Agendamento(), new Agendamento()));

        List<Agendamento> lista = agendamentoService.listarAgendamentosCliente();
        assertEquals(2, lista.size());
    }

    @Test
    void listarAgendamentosPrestador_deveRetornarLista() {
        Prestador prestador = criarPrestador(2, criarNegocio(10, true));
        prestador.setPerfil(PerfilUsuario.PRESTADOR);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);

        when(agendamentoRepository.findByPrestadorId(prestador.getId()))
                .thenReturn(List.of(new Agendamento()));

        List<Agendamento> lista = agendamentoService.listarAgendamentosPrestador();
        assertEquals(1, lista.size());
    }


    @Test
    void listarClientesDoPrestador_deveGerarResumo() {
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);

        Cliente c1 = criarCliente(1);
        Cliente c2 = criarCliente(2);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento a1 = Agendamento.builder()
                .id(1).cliente(c1).prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .status(StatusAgendamento.PENDENTE)
                .build();

        Agendamento a2 = Agendamento.builder()
                .id(2).cliente(c1).prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .status(StatusAgendamento.CANCELADO)
                .build();

        Agendamento a3 = Agendamento.builder()
                .id(3).cliente(c2).prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findByPrestador_Negocio_Id(negocio.getId()))
                .thenReturn(List.of(a1, a2, a3));

        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), c1.getId()))
                .thenReturn(Optional.of(
                        ClienteBloqueado.builder().negocio(negocio).cliente(c1).ativo(true).build()
                ));

        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), c2.getId()))
                .thenReturn(Optional.empty());

        List<ClienteResumoDTO> resumos = agendamentoService.listarClientesDoPrestador();

        assertEquals(2, resumos.size());
        ClienteResumoDTO r1 = resumos.stream().filter(r -> r.getId().equals(c1.getId())).findFirst().orElseThrow();
        assertTrue(r1.isBloqueado());
        assertEquals(50.0, r1.getTaxaCancelamento()); // 2 de 3 cancelados
    }

    @Test
    void listarClientesBloqueados_deveRetornarSomenteBloqueados() {
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);

        Cliente c1 = criarCliente(1);
        Cliente c2 = criarCliente(2);

        ClienteBloqueado b1 = ClienteBloqueado.builder()
                .id(1).negocio(negocio).cliente(c1).ativo(true).build();
        ClienteBloqueado b2 = ClienteBloqueado.builder()
                .id(2).negocio(negocio).cliente(c2).ativo(false).build();

        when(clienteBloqueadoRepository.findByNegocioId(negocio.getId()))
                .thenReturn(List.of(b1, b2));

        Servico servico = criarServico(3, prestador, negocio, true, 30);
        Agendamento a1 = Agendamento.builder()
                .id(1).cliente(c1).prestador(prestador).servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .status(StatusAgendamento.CANCELADO).build();

        when(agendamentoRepository.findByPrestador_Negocio_Id(negocio.getId()))
                .thenReturn(List.of(a1));

        List<ClienteResumoDTO> resumos = agendamentoService.listarClientesBloqueados();

        assertEquals(1, resumos.size());
        assertEquals(c1.getId(), resumos.get(0).getId());
        assertTrue(resumos.get(0).isBloqueado());
    }


    @Test
    void bloquearCliente_deveCriarBloqueioECancelarPendentes() {
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);

        Cliente cliente = criarCliente(1);

        when(usuarioRepository.findById(cliente.getId()))
                .thenReturn(Optional.of(cliente));
        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), cliente.getId()))
                .thenReturn(Optional.empty());

        Servico servico = criarServico(3, prestador, negocio, true, 30);

        Agendamento pendente = Agendamento.builder()
                .id(10).cliente(cliente).prestador(prestador).servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .status(StatusAgendamento.PENDENTE).build();

        Agendamento concluido = Agendamento.builder()
                .id(11).cliente(cliente).prestador(prestador).servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .status(StatusAgendamento.CONCLUIDO).build();

        when(agendamentoRepository.findByPrestadorId(prestador.getId()))
                .thenReturn(List.of(pendente, concluido));

        agendamentoService.bloquearCliente(cliente.getId());

        verify(clienteBloqueadoRepository).save(any(ClienteBloqueado.class));
        assertEquals(StatusAgendamento.CANCELADO, pendente.getStatus());
        assertEquals(StatusAgendamento.CONCLUIDO, concluido.getStatus());
    }

    @Test
    void desbloquearCliente_deveDesativarBloqueio() {
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        usuarioAutenticadoMock.when(UsuarioAutenticado::get).thenReturn(prestador);

        Cliente cliente = criarCliente(1);

        ClienteBloqueado bloqueio = ClienteBloqueado.builder()
                .id(1).negocio(negocio).cliente(cliente).ativo(true).build();

        when(clienteBloqueadoRepository.findByNegocioIdAndClienteId(negocio.getId(), cliente.getId()))
                .thenReturn(Optional.of(bloqueio));

        agendamentoService.desbloquearCliente(cliente.getId());

        assertFalse(bloqueio.isAtivo());
        verify(clienteBloqueadoRepository).save(bloqueio);
    }


    @Test
    void concluirAgendamentosVencidos_deveConcluirQuandoPassouHorario() {
        Cliente cliente = criarCliente(1);
        Negocio negocio = criarNegocio(10, true);
        Prestador prestador = criarPrestador(2, negocio);
        Servico servico = criarServico(3, prestador, negocio, true, 30);

        // agendamento que terminou a uma hora
        Agendamento agVencido = Agendamento.builder()
                .id(1).cliente(cliente).prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(2))
                .status(StatusAgendamento.PENDENTE).build();

        // agendamento futuro
        Agendamento agFuturo = Agendamento.builder()
                .id(2).cliente(cliente).prestador(prestador)
                .servico(servico)
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusHours(2))
                .status(StatusAgendamento.PENDENTE).build();

        when(agendamentoRepository.findByStatus(StatusAgendamento.PENDENTE))
                .thenReturn(List.of(agVencido, agFuturo));

        agendamentoService.concluirAgendamentosVencidos();

        assertEquals(StatusAgendamento.CONCLUIDO, agVencido.getStatus());
        assertEquals(StatusAgendamento.PENDENTE, agFuturo.getStatus());
    }
}
