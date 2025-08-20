package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Administrador;
import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.RegistroUsuarioDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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
                if (dto.getCep() == null || dto.getEndereco() == null) {
                    throw new IllegalArgumentException("CEP e Endereço são obrigatórios para clientes.");
                }
                cliente.setCep(dto.getCep());
                cliente.setEndereco(dto.getEndereco());
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

}
