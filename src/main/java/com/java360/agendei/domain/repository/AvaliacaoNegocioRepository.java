package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.AvaliacaoNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvaliacaoNegocioRepository extends JpaRepository<AvaliacaoNegocio, Integer> {
    List<AvaliacaoNegocio> findByNegocioId(Integer negocioId);
    boolean existsByAgendamentoId(Integer agendamentoId);
}
