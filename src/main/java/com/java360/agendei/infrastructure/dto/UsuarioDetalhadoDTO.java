package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.model.PerfilUsuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioDetalhadoDTO {

    private Integer id;
    private String nome;
    private String email;
    private String telefone;
    private PerfilUsuario perfil;
    private boolean ativo;

    // Apenas Cliente
    private String cep;
    private String endereco;

    // Apenas Prestador
    private NegocioResumoDTO negocio; // 👈 negócio vinculado (resumo)

    public static UsuarioDetalhadoDTO fromEntity(Usuario usuario) {
        String cep = null;
        String endereco = null;
        NegocioResumoDTO negocio = null;

        if (usuario instanceof Cliente cliente) {
            cep = cliente.getCep();
            endereco = cliente.getEndereco();
        } else if (usuario instanceof Prestador prestador && prestador.getNegocio() != null) {
            negocio = NegocioResumoDTO.fromEntity(prestador.getNegocio());
        }

        return UsuarioDetalhadoDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .perfil(usuario.getPerfil())
                .ativo(usuario.isAtivo())
                .cep(cep)
                .endereco(endereco)
                .negocio(negocio)
                .build();
    }
}
