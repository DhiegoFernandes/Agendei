package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import lombok.Data;

@Data
public class UsuarioDTO {
    private final Integer id;
    private final String nome;
    private final String email;
    private final String telefone;
    private final PerfilUsuario perfil;

    public static UsuarioDTO fromEntity(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getPerfil()
        );
    }
}
