package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Cliente;
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
    private final boolean ativo;

    // Apenas para cliente
    private final String cep;
    private final String endereco;

    public static UsuarioDTO fromEntity(Usuario usuario) {
        String cep = null;
        String endereco = null;

        // Se for cliente
        if (usuario instanceof Cliente cliente) {
            cep = cliente.getCep();
            endereco = cliente.getEndereco();
        }

        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                cep,
                endereco
        );
    }
}
