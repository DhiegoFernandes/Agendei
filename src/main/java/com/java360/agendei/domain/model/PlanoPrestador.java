package com.java360.agendei.domain.model;

import lombok.Getter;

@Getter
public enum PlanoPrestador {
    BASICO(1, 1, 49.90),
    INTERMEDIARIO(2, 3, 99.90),
    AVANCADO(3, 5, 199.90);

    private final int nivel;
    private final int limiteConvites;
    private final double valorMensal;

    PlanoPrestador(int nivel, int limiteConvites, double valorMensal) {
        this.nivel = nivel;
        this.limiteConvites = limiteConvites;
        this.valorMensal = valorMensal;
    }
}
