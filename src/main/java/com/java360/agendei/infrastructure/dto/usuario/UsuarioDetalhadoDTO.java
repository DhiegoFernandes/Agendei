package com.java360.agendei.infrastructure.dto.usuario;

import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.PlanoPrestador;
import com.java360.agendei.infrastructure.dto.negocio.NegocioResumoDTO;
import lombok.Builder;
import lombok.Data;

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
    private String fotoPerfilUrl;
    private PlanoPrestador plano;

    public static UsuarioDetalhadoDTO fromEntity(Usuario usuario) {
        String cep = null;
        String endereco = null;
        String numero = null;

        NegocioResumoDTO negocio = null;
        String fotoPerfilUrl = null;
        PlanoPrestador plano = null;

        if (usuario instanceof Cliente cliente) {
            cep = cliente.getCep();
            endereco = cliente.getEndereco();
            numero = cliente.getNumero();

        } else if (usuario instanceof Prestador prestador) {

            if (prestador.getNegocio() != null) {
                negocio = NegocioResumoDTO.fromEntity(prestador.getNegocio());
            }

            if (prestador.getFotoPerfil() != null) {
                fotoPerfilUrl = "/usuarios/" + prestador.getId() + "/foto-perfil";
            }
            plano = prestador.getPlano();
        }

        return UsuarioDetalhadoDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .perfil(usuario.getPerfil())
                .ativo(usuario.isAtivo())

                // Cliente
                .cep(cep)
                .endereco(endereco)
                .numero(numero)

                // Prestador
                .negocio(negocio)
                .fotoPerfilUrl(fotoPerfilUrl)
                .plano(plano)

                .build();
    }
}
