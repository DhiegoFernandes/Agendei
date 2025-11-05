package com.java360.agendei.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificarCodigoDTO {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String codigo;
}
