package com.java360.agendei.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prestadores")
@Data
public class Prestador extends Usuario {

    @Column(length = 255)
    private String bio;

    @ManyToOne
    @JoinColumn(name = "negocio_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude // evita loop de referencia infinita
    private Negocio negocio;

}