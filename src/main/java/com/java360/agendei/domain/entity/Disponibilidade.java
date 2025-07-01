package com.java360.agendei.domain.entity;

import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "disponibilidades")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Disponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaSemanaDisponivel diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;
}