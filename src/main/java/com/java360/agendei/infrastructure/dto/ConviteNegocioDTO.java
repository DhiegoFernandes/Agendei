package com.java360.agendei.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConviteNegocioDTO {

    @NotBlank
    private String negocioId;

    @Email
    @NotBlank
    private String emailPrestador;

    @NotBlank
    private String idDonoNegocio;
}
