package com.java360.agendei.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java360.agendei.domain.model.CategoriaNegocio;
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

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(nullable = false, length = 9)
    private String cep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaNegocio categoria;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "nota_media")
    private Double notaMedia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "criador_id")
    @ToString.Exclude //evita loop de referencia infinita
    @JsonIgnore
    private Prestador criador;
}