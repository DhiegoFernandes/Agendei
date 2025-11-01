package com.java360.agendei.infrastructure.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequestDTO {
    @NotBlank
    private String token;
}
