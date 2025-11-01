package com.java360.agendei.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "prestadores")
@Data
public class Prestador extends Usuario {

    @ManyToOne
    @JoinColumn(name = "negocio_id")
    @ToString.Exclude // evita loop de referencia infinita
    @JsonIgnore
    private Negocio negocio;

    @Lob
    @Column(name = "foto_perfil", columnDefinition = "MEDIUMBLOB")
    @ToString.Exclude
    private byte[] fotoPerfil;

}