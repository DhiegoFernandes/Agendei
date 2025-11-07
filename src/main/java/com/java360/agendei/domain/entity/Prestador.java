package com.java360.agendei.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java360.agendei.domain.model.PlanoPrestador;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "prestadores")
@Data
@EqualsAndHashCode(callSuper = true)
public class Prestador extends Usuario {

    @ManyToOne
    @JoinColumn(name = "negocio_id")
    @JsonIgnore
    @ToString.Exclude // evita loop de referencia infinita
    private Negocio negocio;

    @Lob
    @Column(name = "foto_perfil", columnDefinition = "MEDIUMBLOB")
    private byte[] fotoPerfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano", nullable = false)
    private PlanoPrestador plano = PlanoPrestador.BASICO; // Por padrão o prestador é criado com o plano basico

    @Column(name = "hora_inicio_almoco")
    private LocalTime horaInicioAlmoco;

    @Column(name = "hora_fim_almoco")
    private LocalTime horaFimAlmoco;
}
