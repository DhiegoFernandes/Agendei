package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.model.CategoriaServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, String> {

    List<Servico> findAllByAtivoTrue();

    boolean existsByTituloAndPrestadorId(String titulo, String prestadorId);

    boolean existsByTituloAndPrestadorIdAndIdNot(String titulo, String prestadorId, String id);

    List<Servico> findByNegocio_NomeIgnoreCaseAndAtivoTrue(String nome);

    boolean existsByTituloAndNegocioId(String titulo, String negocioId);

    List<Servico> findByPrestadorIdAndNegocioId(String prestadorId, String negocioId);

    List<Servico> findByNegocio_IdAndAtivoTrue(String negocioId);

    List<Servico> findByPrestadorIdAndNegocioIdAndAtivoTrue(String prestadorId, String negocioId);

    @Query("""
    SELECT s FROM Servico s
    WHERE s.ativo = true
    AND (:titulo IS NULL OR LOWER(s.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
    AND (:categoria IS NULL OR s.categoria = :categoria)
    AND (:nomePrestador IS NULL OR LOWER(s.prestador.nome) LIKE LOWER(CONCAT('%', :nomePrestador, '%')))
    """)
    List<Servico> buscarServicos(
            @Param("titulo") String titulo,
            @Param("categoria") CategoriaServico categoria,
            @Param("nomePrestador") String nomePrestador
    );




}