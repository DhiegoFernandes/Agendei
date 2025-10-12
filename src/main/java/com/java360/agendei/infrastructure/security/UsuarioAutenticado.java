package com.java360.agendei.infrastructure.security;

import com.java360.agendei.domain.entity.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;

public class UsuarioAutenticado {
    public static Usuario get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuario usuario) {
            System.out.println(usuario);
            return usuario;
        }
        throw new SecurityException("Usuário não autenticado.");
    }

    public static Integer getId() {
        return get().getId();
    }
}
