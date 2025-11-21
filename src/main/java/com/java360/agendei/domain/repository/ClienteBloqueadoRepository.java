package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.ClienteBloqueado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteBloqueadoRepository extends JpaRepository<ClienteBloqueado, Integer> {
    Optional<ClienteBloqueado> findByNegocioIdAndClienteId(Integer negocioId, Integer clienteId);

    List<ClienteBloqueado> findByNegocioId(Integer negocioId);

}
