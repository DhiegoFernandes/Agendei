package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Integer> {

    List<Servico> findAllByAtivoTrue();

    boolean existsByTituloAndPrestadorIdAndIdNot(String titulo, Integer prestadorId, Integer id);

    List<Servico> findByNegocio_NomeIgnoreCaseAndAtivoTrue(String nome);

    boolean existsByTituloAndNegocioId(String titulo, Integer negocioId);

    List<Servico> findByPrestadorIdAndNegocioId(Integer prestadorId, Integer negocioId);

    List<Servico> findByNegocio_IdAndAtivoTrue(Integer negocioId);

    List<Servico> findByNegocio_Id(Integer negocioId);

    @Query("""
        SELECT s FROM Servico s
        JOIN s.prestador p
        WHERE s.ativo = true
        AND (:titulo IS NULL OR LOWER(s.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
        AND (:nomePrestador IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :nomePrestador, '%')))
        AND EXISTS (
            SELECT 1 FROM Disponibilidade d
            WHERE d.prestador.id = p.id
            AND (:diaSemana IS NULL OR d.diaSemana = :diaSemana)
            AND d.ativo = true
        )
        """)
    List<Servico> buscarServicos(
            @Param("titulo") String titulo,
            @Param("nomePrestador") String nomePrestador,
            @Param("diaSemana") DiaSemanaDisponivel diaSemana
    );


}