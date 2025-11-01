package com.java360.agendei.infrastructure.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequestDTO {
    @Email
    private String email;

    @NotBlank
    private String senha;
}