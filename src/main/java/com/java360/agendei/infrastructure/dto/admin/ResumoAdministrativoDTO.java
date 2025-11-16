package com.java360.agendei.infrastructure.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResumoAdministrativoDTO {

    private long totalPrestadores;
    private long totalClientes;
    private long totalServicosAtivos;
    private long totalNegociosAtivos;
    private long totalAgendamentos;
}