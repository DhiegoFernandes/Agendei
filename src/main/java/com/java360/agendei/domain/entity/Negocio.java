package com.java360.agendei.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "negocios", uniqueConstraints = @UniqueConstraint(columnNames = "nome"))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    @Column(nullable = false, length = 200)
    private String endereco;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "criador_id")
    @ToString.Exclude //evita loop de referencia infinita
    private Prestador criador;
}