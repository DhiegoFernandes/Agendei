package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestadorRepository extends JpaRepository<Prestador, String> {
    List<Prestador> findByNegocio_Id(String negocioId);
}