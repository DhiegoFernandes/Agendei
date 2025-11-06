package com.java360.agendei.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClienteResumoDTO {
    private Integer id;
    private String nome;
    private String email;
    private String telefone;
    private boolean bloqueado;
}