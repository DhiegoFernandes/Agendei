package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.model.AgendamentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {
    boolean existsByServico_Prestador_IdAndDataHoraAndStatusIn(
            String prestadorId, LocalDateTime dataHora, List<AgendamentoStatus> status);

    List<Agendamento> findByServico_Prestador_IdAndStatusIn(String prestadorId, List<AgendamentoStatus> status);
}