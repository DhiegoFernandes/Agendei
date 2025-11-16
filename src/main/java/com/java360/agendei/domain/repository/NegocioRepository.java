package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NegocioRepository extends JpaRepository<Negocio, Integer> {
    boolean existsByNome(String nome);

    Optional<Negocio> findByNome(String nome);

    List<Negocio> findByAtivoTrue();

    Optional<Negocio> findByNomeAndAtivoTrue(String nome);

    long countByAtivoTrue();

}