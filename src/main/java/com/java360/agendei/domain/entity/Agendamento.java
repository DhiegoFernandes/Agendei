package com.java360.agendei.domain.entity;

import com.java360.agendei.domain.model.AgendamentoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "agendamento")
@Data
@Builder
@AllArgsConstructor //Construtor com todos argumentos
@NoArgsConstructor // Construtor padrão (sem argumentos)
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", nullable = false, length = 45)
    private String name;

    private String description; //######### tirar
    private LocalDate initialDate;
    private LocalDate finalDate;

    @Enumerated(EnumType.STRING) // Tip: (EnumType.ORDINAL) deixa os status PENDING = 0, IN_PROGRESS = 1, FINISHED = 2
    private AgendamentoStatus status;

}
