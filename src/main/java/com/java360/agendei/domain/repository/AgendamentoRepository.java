package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {


}
