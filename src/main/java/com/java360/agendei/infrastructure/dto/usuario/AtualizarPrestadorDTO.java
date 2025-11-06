package com.java360.agendei.infrastructure.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtualizarPrestadorDTO {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, max = 80)
    private String nome;

    @Email(message = "E-mail inválido.")
    @NotBlank(message = "O e-mail é obrigatório.")
    private String email;

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "\\d{10,15}", message = "Telefone deve conter apenas números, com DDD.")
    private String telefone;
}