package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Disponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, String> {
    List<Disponibilidade> findByPrestadorId(String prestadorId);
}
