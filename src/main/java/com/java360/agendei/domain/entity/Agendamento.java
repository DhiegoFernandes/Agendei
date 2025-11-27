package com.java360.agendei.domain.entity;

import com.java360.agendei.domain.model.StatusAgendamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agendamento_prestador_cliente_horario",
                columnNames = {"prestador_id", "cliente_id", "dataHora"}
        ))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    private Servico servico;

    @ManyToOne(optional = false)
    private Prestador prestador;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status;
}
