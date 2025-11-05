package com.java360.agendei.infrastructure.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NovaSenhaDTO {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String codigo;

    @NotBlank
    @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres.")
    private String novaSenha;
}
