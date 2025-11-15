package com.java360.agendei.infrastructure.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtualizarClienteDTO {

    @NotBlank
    @Size(max = 80)
    private String nome;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telefone;


    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido. Use o formato 00000000 ou 00000-000.")
    private String cep;

    @NotBlank
    private String endereco;

    @NotBlank
    private String numero;
}
