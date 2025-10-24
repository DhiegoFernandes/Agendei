package com.java360.agendei.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente extends Usuario {

    @Column(nullable = false, length = 9) // CEP no formato 00000-000
    private String cep;

    @Column(nullable = false, length = 200)
    private String endereco;

    @Column(nullable = false, length = 10)
    private String numero;
}
