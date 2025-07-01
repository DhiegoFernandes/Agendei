package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, String> {

    List<Servico> findAllByAtivoTrue();

    boolean existsByTituloAndPrestadorId(String titulo, String prestadorId);

    boolean existsByTituloAndPrestadorIdAndIdNot(String titulo, String prestadorId, String id);
}