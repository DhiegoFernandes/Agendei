package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.CodigoRecuperacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoRecuperacaoRepository extends JpaRepository<CodigoRecuperacao, Integer> {
    Optional<CodigoRecuperacao> findByEmail(String email);
    Optional<CodigoRecuperacao> findByEmailAndCodigo(String email, String codigo);
    void deleteByEmail(String email);
}