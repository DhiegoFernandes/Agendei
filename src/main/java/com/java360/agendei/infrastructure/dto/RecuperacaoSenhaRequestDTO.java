package com.java360.agendei.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecuperacaoSenhaRequestDTO {
    @NotBlank
    @Email
    private String email;
}
