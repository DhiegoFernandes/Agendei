package com.java360.agendei.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clientes_bloqueados",
        uniqueConstraints = @UniqueConstraint(columnNames = {"prestador_id", "cliente_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteBloqueado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;
}