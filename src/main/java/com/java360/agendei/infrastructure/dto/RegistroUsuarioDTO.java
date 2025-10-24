package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.PerfilUsuario;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroUsuarioDTO {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, max = 80)
    private String nome;

    @Email
    @Email(message = "E-mail inválido.")
    private String email;

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "\\d{10,15}", message = "Telefone deve conter apenas números, com DDD.")
    private String telefone;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    private String senha;

    @NotNull(message = "O perfil é obrigatório.")
    private PerfilUsuario perfil;

    @Size(max = 9, message = "CEP deve ter no máximo 9 caracteres.")
    private String cep;

    private String endereco;

    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres.")
    private String numero;
}
