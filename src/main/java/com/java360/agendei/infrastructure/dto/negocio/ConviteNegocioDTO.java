package com.java360.agendei.infrastructure.dto.negocio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConviteNegocioDTO {

    @Email
    @NotBlank
    private String emailPrestador;
}
