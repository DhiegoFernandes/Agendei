package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Administrador;
import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.*;
import com.java360.agendei.infrastructure.security.JwtService;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public Usuario registrarUsuario(RegistroUsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("E-mail já está em uso.");
        }

        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());

        Usuario usuario;
        switch (dto.getPerfil()) {
            case CLIENTE -> {
                Cliente cliente = new Cliente();
                if (dto.getCep() == null || dto.getEndereco() == null || dto.getNumero() == null) {
                    throw new IllegalArgumentException("CEP, Endereço e Número são obrigatórios para clientes.");
                }
                cliente.setCep(dto.getCep());
                cliente.setEndereco(dto.getEndereco());
                cliente.setNumero(dto.getNumero());
                usuario = cliente;
            }
            case PRESTADOR -> usuario = new Prestador();
            case ADMIN -> usuario = new Administrador();
            default -> throw new IllegalArgumentException("Perfil inválido.");
        }

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefone(dto.getTelefone());
        usuario.setSenha(senhaCriptografada);
        usuario.setAtivo(true);
        usuario.setPerfil(dto.getPerfil());

        return usuarioRepository.save(usuario);
    }

    public UsuarioDetalhadoDTO buscarDadosUsuarioPorToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Integer userId = jwtService.extractUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException("Token inválido.");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        return UsuarioDetalhadoDTO.fromEntity(usuario);
    }

    @Transactional
    public Page<UsuarioDetalhadoDTO> listarTodosUsuariosPaginado(String token, int page, int size, String sortBy, String direction) {
        // Remove o prefixo "Bearer " do token
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Extrai o ID do usuário logado
        Integer userId = jwtService.extractUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException("Token inválido.");
        }

        // Busca o usuário logado
        Usuario usuarioLogado = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // Verifica se é ADMIN
        if (usuarioLogado.getPerfil() != com.java360.agendei.domain.model.PerfilUsuario.ADMIN) {
            throw new SecurityException("Acesso negado. Apenas administradores podem listar todos os usuários.");
        }

        // Configura ordenação
        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Busca usuários com paginação
        Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);

        // Converte para DTO
        return usuariosPage.map(UsuarioDetalhadoDTO::fromEntity);
    }


    @Transactional
    public Page<UsuarioDetalhadoDTO> listarUsuariosComFiltros(
            String token,
            int page,
            int size,
            String sortBy,
            String direction,
            com.java360.agendei.domain.model.PerfilUsuario perfil,
            String nome,
            String email,
            String telefone
    ) {
        // Remove prefixo "Bearer "
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Extrai ID do usuário
        Integer userId = jwtService.extractUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException("Token inválido.");
        }

        Usuario usuarioLogado = usuarioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (usuarioLogado.getPerfil() != com.java360.agendei.domain.model.PerfilUsuario.ADMIN) {
            throw new SecurityException("Acesso negado. Apenas administradores podem listar usuários.");
        }

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Busca com filtros opcionais
        Page<Usuario> usuariosPage = usuarioRepository.buscarComFiltros(perfil, nome, email, telefone, pageable);

        return usuariosPage.map(UsuarioDetalhadoDTO::fromEntity);
    }

    @Transactional
    public void atualizarFotoPerfil(MultipartFile arquivo) {
        Usuario usuario = UsuarioAutenticado.get();

        if (!(usuario instanceof Prestador prestador)) {
            throw new IllegalArgumentException("Somente prestadores podem ter foto de perfil.");
        }

        try {
            if (arquivo.isEmpty()) {
                throw new IllegalArgumentException("Arquivo de imagem inválido ou vazio.");
            }

            byte[] bytes = arquivo.getBytes();
            prestador.setFotoPerfil(bytes);
            usuarioRepository.save(prestador);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar a imagem: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public FotoPrestadorDTO buscarFotoPerfilDTO(Integer prestadorId) {
        Usuario usuario = usuarioRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        if (!(usuario instanceof Prestador prestador)) {
            throw new IllegalArgumentException("Usuário não é um prestador.");
        }

        if (prestador.getFotoPerfil() == null) {
            throw new IllegalArgumentException("Prestador não possui foto de perfil cadastrada.");
        }

        return new FotoPrestadorDTO(
                prestador.getId(),
                prestador.getNome(),
                "/usuarios/" + prestador.getId() + "/foto-perfil"
        );
    }

    @Transactional(readOnly = true)
    public byte[] buscarFotoPerfilBytes(Integer prestadorId) {
        Usuario usuario = usuarioRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        if (!(usuario instanceof Prestador prestador)) {
            throw new IllegalArgumentException("Usuário não é um prestador.");
        }

        if (prestador.getFotoPerfil() == null) {
            throw new IllegalArgumentException("Prestador não possui foto de perfil cadastrada.");
        }

        return prestador.getFotoPerfil();
    }




}
