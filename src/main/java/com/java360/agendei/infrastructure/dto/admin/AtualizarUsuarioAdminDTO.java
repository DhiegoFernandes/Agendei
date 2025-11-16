package com.java360.agendei.infrastructure.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtualizarUsuarioAdminDTO {

    @NotBlank
    @Size(max = 80)
    private String nome;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telefone;

    private Boolean ativo;

    // Campos exclusivos de clientes
    private String cep;
    private String endereco;
    private String numero;
}