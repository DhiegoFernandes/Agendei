package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.PerfilUsuario;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroUsuarioDTO {

    @NotBlank
    @Size(min = 3, max = 80)
    private String nome;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{10,15}", message = "Telefone deve conter apenas números, com DDD.")
    private String telefone;

    @NotBlank
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    private String senha;

    @NotNull
    private PerfilUsuario perfil;

    private String cep;

    private String endereco;
}
