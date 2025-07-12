package com.java360.agendei.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "prestadores")
@Data
public class Prestador extends Usuario {

    @Column(length = 255)
    private String bio;

    @ManyToOne
    @JoinColumn(name = "negocio_id")
    @ToString.Exclude // evita loop de referencia infinita
    private Negocio negocio;

    // Exemplo: relação com serviços prestados
    // @OneToMany(mappedBy = "prestador")
    // private List<Servico> servicos;

}