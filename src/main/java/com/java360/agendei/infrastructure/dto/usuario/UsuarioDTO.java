package com.java360.agendei.infrastructure.dto.usuario;

import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
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
    private final String numero;

    public static UsuarioDTO fromEntity(Usuario usuario) {
        String cep = null;
        String endereco = null;
        String numero = null;

        // Se for cliente
        if (usuario instanceof Cliente cliente) {
            cep = cliente.getCep();
            endereco = cliente.getEndereco();
            numero = cliente.getNumero();
        }

        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .perfil(usuario.getPerfil())
                .ativo(usuario.isAtivo())
                .cep(cep)
                .endereco(endereco)
                .numero(numero)
                .build();
    }
}
