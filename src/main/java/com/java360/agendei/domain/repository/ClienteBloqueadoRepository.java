package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.ClienteBloqueado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteBloqueadoRepository extends JpaRepository<ClienteBloqueado, Integer> {
    Optional<ClienteBloqueado> findByPrestadorIdAndClienteId(Integer prestadorId, Integer clienteId);
    List<ClienteBloqueado> findByPrestadorId(Integer prestadorId);
}
