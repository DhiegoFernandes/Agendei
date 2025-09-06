package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Integer> {
    List<Agendamento> findByClienteId(Integer clienteId);
    List<Agendamento> findByPrestadorId(Integer prestadorId);
    boolean existsByPrestadorIdAndDataHoraBetween(Integer prestadorId, LocalDateTime inicio, LocalDateTime fim);


    // Verifica se o horário está livre (sem colisões)
    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.prestador.id = :prestadorId AND a.dataHora BETWEEN :inicio AND :fim")
    boolean existeAgendamentoNoHorario(@Param("prestadorId") Integer prestadorId,
                                       @Param("inicio") LocalDateTime inicio,
                                       @Param("fim") LocalDateTime fim);

    List<Agendamento> findByPrestadorIdAndDataHoraBetween(Integer prestadorId,
                                                          LocalDateTime inicio,
                                                          LocalDateTime fim);

    List<Agendamento> findByPrestadorIdAndStatusAndDataHoraBetween(Integer prestadorId,
                                                                   StatusAgendamento status,
                                                                   LocalDateTime inicio,
                                                                   LocalDateTime fim);

}
