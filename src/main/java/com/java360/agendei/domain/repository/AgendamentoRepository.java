package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Integer> {
    List<Agendamento> findByClienteId(Integer clienteId);
    List<Agendamento> findByPrestadorId(Integer prestadorId);
    boolean existsByPrestadorIdAndDataHora(Integer prestadorId, LocalDateTime dataHora);
}
