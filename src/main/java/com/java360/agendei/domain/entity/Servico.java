package com.java360.agendei.domain.entity;

import com.java360.agendei.domain.model.CategoriaServico;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;
}
