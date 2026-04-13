package com.java360.agendei.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fotos_negocio")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FotoNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "BYTEA")
    @ToString.Exclude
    private byte[] imagem;

    @Column(nullable = false)
    private String nomeArquivo; // Para saber a extensão/nome do arquivo

    @ManyToOne(optional = false)
    @JoinColumn(name = "negocio_id")
    @ToString.Exclude
    private Negocio negocio;
}
