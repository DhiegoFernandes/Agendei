package com.java360.agendei.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "prestadores")
@Data
public class Prestador extends Usuario {

    @Column(length = 255)
    private String bio;

    @ManyToOne
    @JoinColumn(name = "negocio_id")
    //@EqualsAndHashCode.Exclude
    @ToString.Exclude // evita loop de referencia infinita
    @JsonIgnore
    private Negocio negocio;

}