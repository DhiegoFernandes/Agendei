package com.java360.agendei.service;

import com.java360.agendei.domain.applicationservice.FotoNegocioService;
import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.repository.FotoNegocioRepository;
import com.java360.agendei.domain.repository.NegocioRepository;
import com.java360.agendei.infrastructure.dto.negocio.FotoNegocioDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FotoNegocioServiceTest {

    @Mock
    private NegocioRepository negocioRepo;

    @Mock
    private FotoNegocioRepository fotoRepo;

    @InjectMocks
    private FotoNegocioService service;

    private Prestador dono;
    private Negocio negocio;
    private MockedStatic<UsuarioAutenticado> mockAuth;
    private MockedStatic<PermissaoUtils> mockPerm;

    @BeforeEach
    void setup() {
        dono = new Prestador();
        dono.setId(1);

        negocio = new Negocio();
        negocio.setId(10);
        negocio.setCriador(dono);

        mockAuth = Mockito.mockStatic(UsuarioAutenticado.class);
        mockPerm = Mockito.mockStatic(PermissaoUtils.class);

        mockAuth.when(UsuarioAutenticado::get).thenReturn(dono);
        mockPerm.when(() -> PermissaoUtils.validarPermissao(any(), any())).then(i -> null);
        mockPerm.when(() -> PermissaoUtils.isAdmin(any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        mockAuth.close();
        mockPerm.close();
    }

    @Test
    void adicionarFoto_sucesso_donoDoNegocio() throws Exception {
        MultipartFile file = new MockMultipartFile("f", "foto.jpg", "image/jpeg", "abc".getBytes());

        when(negocioRepo.findById(10)).thenReturn(Optional.of(negocio));

        service.adicionarFotoAoNegocio(10, file);

        verify(fotoRepo).save(any());
    }

    @Test
    void adicionarFoto_negocioNaoExiste() {
        MultipartFile file = new MockMultipartFile("f", "foto.jpg", "image/jpeg", "abc".getBytes());

        when(negocioRepo.findById(10)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.adicionarFotoAoNegocio(10, file));
    }

    @Test
    void adicionarFoto_usuarioNaoEDonoENaoAdmin() {
        Prestador outro = new Prestador();
        outro.setId(999);
        mockAuth.when(UsuarioAutenticado::get).thenReturn(outro);

        MultipartFile file = new MockMultipartFile("f", "foto.jpg", "image/jpeg", "abc".getBytes());
        when(negocioRepo.findById(10)).thenReturn(Optional.of(negocio));

        assertThrows(IllegalArgumentException.class, () ->
                service.adicionarFotoAoNegocio(10, file));
    }

    @Test
    void adicionarFoto_adminPodeAdicionar() throws Exception {
        mockPerm.when(() -> PermissaoUtils.isAdmin(any())).thenReturn(true);

        MultipartFile file = new MockMultipartFile("f", "foto.png", "image/png", "xxx".getBytes());
        when(negocioRepo.findById(10)).thenReturn(Optional.of(negocio));

        service.adicionarFotoAoNegocio(10, file);

        verify(fotoRepo).save(any());
    }

    @Test
    void adicionarFoto_arquivoVazio() {
        MultipartFile file = new MockMultipartFile("f", "a.png", "image/png", new byte[0]);

        when(negocioRepo.findById(10)).thenReturn(Optional.of(negocio));

        assertThrows(IllegalArgumentException.class, () ->
                service.adicionarFotoAoNegocio(10, file));
    }

    @Test
    void adicionarFoto_tipoInvalido() {
        MultipartFile file = new MockMultipartFile("f", "a.gif", "image/gif", "aaa".getBytes());

        when(negocioRepo.findById(10)).thenReturn(Optional.of(negocio));

        assertThrows(IllegalArgumentException.class, () ->
                service.adicionarFotoAoNegocio(10, file));
    }

    @Test
    void adicionarFoto_tamanhoExcedido() {
        byte[] big = new byte[(6 * 1024 * 1024)];
        MultipartFile file = new MockMultipartFile("f", "a.png", "image/png", big);

        when(negocioRepo.findById(10)).thenReturn(Optional.of(negocio));

        assertThrows(IllegalArgumentException.class, () ->
                service.adicionarFotoAoNegocio(10, file));
    }

    @Test
    void listarFotosDoNegocio_sucesso() {
        FotoNegocio f = FotoNegocio.builder()
                .id(5)
                .nomeArquivo("x.jpg")
                .imagem("abc".getBytes())
                .negocio(negocio)
                .build();

        when(fotoRepo.findByNegocioId(10)).thenReturn(List.of(f));

        List<FotoNegocio> result = service.listarFotosDoNegocio(10);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getId());
    }


    @Test
    void listarFotosDTO_sucesso() {
        FotoNegocio f = FotoNegocio.builder()
                .id(5)
                .nomeArquivo("foto.png")
                .imagem("aaa".getBytes())
                .negocio(negocio)
                .build();

        when(fotoRepo.findByNegocioId(10)).thenReturn(List.of(f));

        List<FotoNegocioDTO> lista = service.listarFotosDoNegocioDTO(10);

        assertEquals(1, lista.size());
        assertEquals("/negocios/10/fotos/5", lista.get(0).getUrl());
    }

    @Test
    void deletarFoto_sucesso_donoDoNegocio() {
        FotoNegocio f = FotoNegocio.builder()
                .id(9)
                .negocio(negocio)
                .imagem("X".getBytes())
                .nomeArquivo("arq")
                .build();

        when(fotoRepo.findById(9)).thenReturn(Optional.of(f));

        service.deletarFoto(10, 9);

        verify(fotoRepo).delete(f);
    }

    @Test
    void deletarFoto_fotoNaoExiste() {
        when(fotoRepo.findById(9)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.deletarFoto(10, 9));
    }

    @Test
    void deletarFoto_fotoNaoPertenceAoNegocio() {
        Negocio n2 = new Negocio();
        n2.setId(999); // id diferente do 10

        FotoNegocio f = FotoNegocio.builder()
                .id(9)
                .negocio(n2) // negocio com id diferente
                .imagem("x".getBytes())
                .nomeArquivo("a")
                .build();

        when(fotoRepo.findById(9)).thenReturn(Optional.of(f));

        assertThrows(IllegalArgumentException.class, () ->
                service.deletarFoto(10, 9));
    }


    @Test
    void deletarFoto_usuarioNaoTemPermissao() {
        Prestador outro = new Prestador();
        outro.setId(999);
        outro.setNegocio(null);

        mockAuth.when(UsuarioAutenticado::get).thenReturn(outro);

        FotoNegocio f = FotoNegocio.builder()
                .id(9)
                .negocio(negocio)
                .imagem("x".getBytes())
                .nomeArquivo("a")
                .build();

        when(fotoRepo.findById(9)).thenReturn(Optional.of(f));

        assertThrows(IllegalArgumentException.class, () ->
                service.deletarFoto(10, 9));
    }

    @Test
    void deletarFoto_adminPode() {
        mockPerm.when(() -> PermissaoUtils.isAdmin(any())).thenReturn(true);

        FotoNegocio f = FotoNegocio.builder()
                .id(9)
                .negocio(negocio)
                .imagem("x".getBytes())
                .nomeArquivo("a")
                .build();

        when(fotoRepo.findById(9)).thenReturn(Optional.of(f));

        service.deletarFoto(10, 9);

        verify(fotoRepo).delete(f);
    }
}
