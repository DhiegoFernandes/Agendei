package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.UsuarioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.PlanoPrestador;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.admin.AtualizarUsuarioAdminDTO;
import com.java360.agendei.infrastructure.dto.usuario.*;
import com.java360.agendei.infrastructure.security.JwtService;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuarioService service;

    @Test
    void registrarUsuario_cliente_sucesso() {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setNome("Cliente Teste");
        dto.setEmail("EMAIL@TESTE.COM");
        dto.setTelefone("11999999999");
        dto.setSenha("Aa123456!");
        dto.setPerfil(PerfilUsuario.CLIENTE);
        dto.setCep("01001-000");
        dto.setEndereco("Rua X");
        dto.setNumero("100");

        when(usuarioRepository.existsByEmail("email@teste.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senhaCripto");
        when(usuarioRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        Usuario u = service.registrarUsuario(dto);

        assertEquals("email@teste.com", u.getEmail());
        assertEquals("senhaCripto", u.getSenha());
    }

    @Test
    void registrarUsuario_emailDuplicado() {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setEmail("a@a.com");
        dto.setTelefone("11999999999");
        dto.setNome("x");
        dto.setSenha("Aa123456!");
        dto.setPerfil(PerfilUsuario.CLIENTE);
        dto.setCep("01001-000");
        dto.setEndereco("Rua X");
        dto.setNumero("10");

        when(usuarioRepository.existsByEmail("a@a.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.registrarUsuario(dto));
    }

    @Test
    void registrarUsuario_senhaInvalida() {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setEmail("a@a.com");
        dto.setTelefone("11999999999");
        dto.setNome("x");
        dto.setSenha("abcdefg"); // invalida
        dto.setPerfil(PerfilUsuario.CLIENTE);
        dto.setCep("01001-000");
        dto.setEndereco("Rua X");
        dto.setNumero("10");

        when(usuarioRepository.existsByEmail("a@a.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.registrarUsuario(dto));
    }

    @Test
    void registrarUsuario_cliente_semEndereco() {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setEmail("a@a.com");
        dto.setTelefone("11999999999");
        dto.setNome("x");
        dto.setSenha("Aa123456!");
        dto.setPerfil(PerfilUsuario.CLIENTE);

        assertThrows(IllegalArgumentException.class, () -> service.registrarUsuario(dto));
    }

    @Test
    void buscarDadosUsuarioPorToken_sucesso() {
        String token = "Bearer abc";
        when(jwtService.extractUserId("abc")).thenReturn(10);

        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setNome("User");

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));

        UsuarioDetalhadoDTO dto = service.buscarDadosUsuarioPorToken(token);

        assertEquals(10, dto.getId());
        assertEquals("User", dto.getNome());
    }

    @Test
    void buscarDadosUsuarioPorToken_invalido() {
        when(jwtService.extractUserId("abc")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.buscarDadosUsuarioPorToken("Bearer abc"));
    }


    @Test
    void listarTodosUsuariosPaginado_admin_sucesso() {
        String token = "Bearer abc";
        when(jwtService.extractUserId("abc")).thenReturn(1);

        Administrador admin = new Administrador();
        admin.setId(1);
        admin.setPerfil(PerfilUsuario.ADMIN);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        Usuario u = new Usuario();
        u.setId(2);
        Page<Usuario> page = new PageImpl<>(List.of(u));

        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<UsuarioDetalhadoDTO> result =
                service.listarTodosUsuariosPaginado(token, 0, 10, "id", "asc");

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listarTodosUsuariosPaginado_semPermissao() {
        String token = "Bearer abc";
        when(jwtService.extractUserId("abc")).thenReturn(1);

        Usuario comum = new Usuario();
        comum.setId(1);
        comum.setPerfil(PerfilUsuario.CLIENTE);

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(comum));

        assertThrows(SecurityException.class,
                () -> service.listarTodosUsuariosPaginado(token, 0, 10, "id", "asc"));
    }

    @Test
    void atualizarDadosCliente_sucesso() {
        Cliente cliente = new Cliente();
        cliente.setId(5);
        cliente.setPerfil(PerfilUsuario.CLIENTE);
        cliente.setEmail("old@a.com");

        try (MockedStatic<UsuarioAutenticado> mock = mockStatic(UsuarioAutenticado.class)) {
            mock.when(UsuarioAutenticado::get).thenReturn(cliente);

            AtualizarClienteDTO dto = new AtualizarClienteDTO();
            dto.setNome("Novo");
            dto.setEmail("novo@a.com");
            dto.setTelefone("11999999999");
            dto.setCep("01001-000");
            dto.setEndereco("Rua X");
            dto.setNumero("100");

            when(usuarioRepository.findByEmail("novo@a.com")).thenReturn(Optional.empty());
            when(usuarioRepository.save(any())).thenAnswer(a -> a.getArgument(0));

            UsuarioDetalhadoDTO result = service.atualizarDadosCliente(dto);

            assertEquals("Novo", result.getNome());
            assertEquals("novo@a.com", result.getEmail());
        }
    }

    @Test
    void atualizarDadosCliente_emailDuplicado() {
        Cliente cliente = new Cliente();
        cliente.setId(5);
        cliente.setPerfil(PerfilUsuario.CLIENTE);
        cliente.setEmail("old@a.com");

        try (MockedStatic<UsuarioAutenticado> mock = mockStatic(UsuarioAutenticado.class)) {
            mock.when(UsuarioAutenticado::get).thenReturn(cliente);

            AtualizarClienteDTO dto = new AtualizarClienteDTO();
            dto.setNome("Novo");
            dto.setEmail("new@x.com");
            dto.setTelefone("119");
            dto.setCep("01001-000");
            dto.setEndereco("Rua X");
            dto.setNumero("1");

            Usuario outro = new Usuario();
            outro.setId(99);

            when(usuarioRepository.findByEmail("new@x.com")).thenReturn(Optional.of(outro));

            assertThrows(IllegalArgumentException.class,
                    () -> service.atualizarDadosCliente(dto));
        }
    }

    @Test
    void atualizarFotoPerfil_sucesso() throws Exception {
        Prestador prestador = new Prestador();
        prestador.setId(3);
        prestador.setPerfil(PerfilUsuario.PRESTADOR);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenReturn("abc".getBytes());

        try (MockedStatic<UsuarioAutenticado> mock = mockStatic(UsuarioAutenticado.class)) {
            mock.when(UsuarioAutenticado::get).thenReturn(prestador);

            service.atualizarFotoPerfil(file);

            verify(usuarioRepository).save(prestador);
            assertArrayEquals("abc".getBytes(), prestador.getFotoPerfil());
        }
    }

    // buca foto do perfil com bytes
    @Test
    void buscarFotoPerfilBytes_sucesso() {
        Prestador p = new Prestador();
        p.setId(10);
        p.setFotoPerfil("img".getBytes());

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(p));

        byte[] result = service.buscarFotoPerfilBytes(10);

        assertArrayEquals("img".getBytes(), result);
    }

    @Test
    void buscarFotoPerfilBytes_semFoto() {
        Prestador p = new Prestador();
        p.setId(10);

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(p));

        assertThrows(IllegalArgumentException.class,
                () -> service.buscarFotoPerfilBytes(10));
    }

    @Test
    void atualizarUsuarioComoAdmin_sucesso() {
        Administrador admin = new Administrador();
        admin.setId(1);
        admin.setPerfil(PerfilUsuario.ADMIN);

        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setPerfil(PerfilUsuario.CLIENTE);

        try (MockedStatic<UsuarioAutenticado> mockAuth = mockStatic(UsuarioAutenticado.class);
             MockedStatic<PermissaoUtils> mockPerm = mockStatic(PermissaoUtils.class)) {

            mockAuth.when(UsuarioAutenticado::get).thenReturn(admin);

            AtualizarUsuarioAdminDTO dto = new AtualizarUsuarioAdminDTO();
            dto.setNome("Novo Nome");
            dto.setEmail("novo@x.com");
            dto.setTelefone("11");
            dto.setAtivo(true);

            when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findByEmail("novo@x.com")).thenReturn(Optional.empty());
            when(usuarioRepository.save(any())).thenAnswer(a -> a.getArgument(0));

            UsuarioDetalhadoDTO result = service.atualizarUsuarioComoAdmin(10, dto);

            assertEquals("Novo Nome", result.getNome());
            assertEquals("novo@x.com", result.getEmail());
        }
    }


    @Test
    void alterarPlanoPrestador_semPermissao() {
        Prestador prest = new Prestador();
        prest.setId(20);
        prest.setPlano(PlanoPrestador.BASICO);

        Prestador usuarioLogado = new Prestador();
        usuarioLogado.setId(99);
        usuarioLogado.setPerfil(PerfilUsuario.PRESTADOR);

        try (MockedStatic<UsuarioAutenticado> mockAuth = mockStatic(UsuarioAutenticado.class);
             MockedStatic<PermissaoUtils> mockPerm = mockStatic(PermissaoUtils.class)) {

            mockAuth.when(UsuarioAutenticado::get).thenReturn(usuarioLogado);
            mockPerm.when(() -> PermissaoUtils.validarPermissao(any(), any(), any()))
                    .thenAnswer(i -> null);

            when(usuarioRepository.findById(20)).thenReturn(Optional.of(prest));

            assertThrows(SecurityException.class,
                    () -> service.alterarPlanoPrestador(20, PlanoPrestador.AVANCADO));
        }
    }

    @Test
    void listarUsuariosComFiltros_semPermissao() {
        String token = "Bearer tok";
        when(jwtService.extractUserId("tok")).thenReturn(3);

        Usuario normal = new Usuario();
        normal.setId(3);
        normal.setPerfil(PerfilUsuario.CLIENTE);

        when(usuarioRepository.findById(3)).thenReturn(Optional.of(normal));

        assertThrows(SecurityException.class,
                () -> service.listarUsuariosComFiltros(token, 0, 10, "id", "asc",
                        null, null, null, null));
    }

    @Test
    void atualizarDadosPrestador_sucesso() {
        Prestador prest = new Prestador();
        prest.setId(5);
        prest.setPerfil(PerfilUsuario.PRESTADOR);
        prest.setEmail("old@x.com");

        try (MockedStatic<UsuarioAutenticado> mock = mockStatic(UsuarioAutenticado.class)) {
            mock.when(UsuarioAutenticado::get).thenReturn(prest);

            AtualizarPrestadorDTO dto = new AtualizarPrestadorDTO();
            dto.setNome("Novo");
            dto.setEmail("novo@x.com");
            dto.setTelefone("1199888");

            when(usuarioRepository.findByEmail("novo@x.com")).thenReturn(Optional.empty());
            when(usuarioRepository.save(any())).thenAnswer(a -> a.getArgument(0));

            UsuarioDetalhadoDTO result = service.atualizarDadosPrestador(dto);

            assertEquals("Novo", result.getNome());
            assertEquals("novo@x.com", result.getEmail());
        }
    }
    @Test
    void atualizarDadosPrestador_emailDuplicado() {
        Prestador prest = new Prestador();
        prest.setId(10);
        prest.setPerfil(PerfilUsuario.PRESTADOR);
        prest.setEmail("old@x.com");

        try (MockedStatic<UsuarioAutenticado> mock = mockStatic(UsuarioAutenticado.class)) {
            mock.when(UsuarioAutenticado::get).thenReturn(prest);

            AtualizarPrestadorDTO dto = new AtualizarPrestadorDTO();
            dto.setNome("XX");
            dto.setEmail("dup@x.com");
            dto.setTelefone("1199");

            Usuario outro = new Usuario();
            outro.setId(99);

            when(usuarioRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(outro));

            assertThrows(IllegalArgumentException.class,
                    () -> service.atualizarDadosPrestador(dto));
        }
    }

    @Test
    void buscarFotoPerfilDTO_sucesso() {
        Prestador p = new Prestador();
        p.setId(10);
        p.setNome("Prest");
        p.setFotoPerfil("abc".getBytes());

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(p));

        FotoPrestadorDTO dto = service.buscarFotoPerfilDTO(10);

        assertEquals(10, dto.getPrestadorId());
        assertEquals("Prest", dto.getNomePrestador());
        assertEquals("/usuarios/10/foto-perfil", dto.getUrlFoto());
    }
    @Test
    void buscarFotoPerfilDTO_usuarioNaoPrestador() {
        Usuario u = new Usuario();
        u.setId(10);

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class,
                () -> service.buscarFotoPerfilDTO(10));
    }
    @Test
    void buscarFotoPerfilDTO_semFoto() {
        Prestador p = new Prestador();
        p.setId(10);

        when(usuarioRepository.findById(10)).thenReturn(Optional.of(p));

        assertThrows(IllegalArgumentException.class,
                () -> service.buscarFotoPerfilDTO(10));
    }
    @Test
    void atualizarUsuarioComoAdmin_emailDuplicado() {
        Administrador admin = new Administrador();
        admin.setId(1);
        admin.setPerfil(PerfilUsuario.ADMIN);

        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("old@x.com");

        try (MockedStatic<UsuarioAutenticado> mockAuth = mockStatic(UsuarioAutenticado.class);
             MockedStatic<PermissaoUtils> mockPerm = mockStatic(PermissaoUtils.class)) {

            mockAuth.when(UsuarioAutenticado::get).thenReturn(admin);

            AtualizarUsuarioAdminDTO dto = new AtualizarUsuarioAdminDTO();
            dto.setNome("Novo");
            dto.setEmail("dup@x.com");
            dto.setTelefone("119");

            Usuario outro = new Usuario();
            outro.setId(99);

            when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findByEmail("dup@x.com")).thenReturn(Optional.of(outro));

            assertThrows(IllegalArgumentException.class,
                    () -> service.atualizarUsuarioComoAdmin(10, dto));
        }
    }



}
