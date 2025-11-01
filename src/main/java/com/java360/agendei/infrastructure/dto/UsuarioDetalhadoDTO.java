package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.model.PerfilUsuario;
import lombok.Builder;
import lombok.Data;

import java.util.Base64;

@Data
@Builder
public class UsuarioDetalhadoDTO {

    // Usuario
    private Integer id;
    private String nome;
    private String email;
    private String telefone;
    private PerfilUsuario perfil;
    private boolean ativo;

    // Apenas Cliente
    private String cep;
    private String endereco;
    private String numero;

    // Apenas Prestador
    private NegocioResumoDTO negocio; // negócio vinculado
    private String fotoPerfilBase64;

    public static UsuarioDetalhadoDTO fromEntity(Usuario usuario) {
        String cep = null;
        String endereco = null;
        String numero = null;
        NegocioResumoDTO negocio = null;
        String fotoPerfilBase64 = null;

        if (usuario instanceof Cliente cliente) {
            cep = cliente.getCep();
            endereco = cliente.getEndereco();
            numero = cliente.getNumero();
        }  else if (usuario instanceof Prestador prestador) {
            if (prestador.getNegocio() != null) {
                negocio = NegocioResumoDTO.fromEntity(prestador.getNegocio());
            }

            if (prestador.getFotoPerfil() != null) {
                fotoPerfilBase64 = "data:image/jpeg;base64," +
                        Base64.getEncoder().encodeToString(prestador.getFotoPerfil());
            }
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
                .numero(numero)
                .negocio(negocio)
                .fotoPerfilBase64(fotoPerfilBase64)
                .build();
    }
}
