package com.java360.agendei.domain.repository;

import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    // Busca filtrada com parâmetros opcionais
    @Query("""
        SELECT u FROM Usuario u
        WHERE (:perfil IS NULL OR u.perfil = :perfil)
        AND (:nome IS NULL OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
        AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:telefone IS NULL OR u.telefone LIKE CONCAT('%', :telefone, '%'))
        """)
    Page<Usuario> buscarComFiltros(
            @Param("perfil") PerfilUsuario perfil,
            @Param("nome") String nome,
            @Param("email") String email,
            @Param("telefone") String telefone,
            Pageable pageable
    );
}