package com.java360.agendei.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConviteNegocioDTO {

    @Email
    @NotBlank
    private String emailPrestador;
}
