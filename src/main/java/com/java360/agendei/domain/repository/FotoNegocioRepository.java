package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.FotoNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoNegocioRepository extends JpaRepository<FotoNegocio, Integer> {
    List<FotoNegocio> findByNegocioId(Integer negocioId);
}