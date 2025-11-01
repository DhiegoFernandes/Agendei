package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.model.PerfilUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String perfil;
    private String nome;
}