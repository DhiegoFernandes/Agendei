package com.java360.agendei.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java360.agendei.domain.model.CategoriaServico;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "servicos")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaServico categoria;

    @Column(nullable = false)
    private double valor;

    @Column(nullable = false)
    private int duracaoMinutos;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestador_id")
    @JsonIgnore
    private Prestador prestador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Negocio negocio;
}
