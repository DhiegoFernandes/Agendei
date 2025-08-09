package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Integer> {
    List<Disponibilidade> findByPrestadorId(Integer prestadorId);

    Optional<Disponibilidade> findByPrestadorIdAndDiaSemana(Integer prestadorId, DiaSemanaDisponivel diaSemana);

}
