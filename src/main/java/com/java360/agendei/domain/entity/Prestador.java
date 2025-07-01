package com.java360.agendei.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prestadores")
@Data
public class Prestador extends Usuario {

    @Column(length = 255)
    private String bio;

    // Exemplo: relação com serviços prestados
    // @OneToMany(mappedBy = "prestador")
    // private List<Servico> servicos;

}