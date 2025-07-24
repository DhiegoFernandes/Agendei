package com.java360.agendei.infrastructure.security;

import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;

public class PermissaoUtils {
    public static boolean isAdmin(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMIN;
    }

    public static void validarPermissao(Usuario usuario, PerfilUsuario... permitidos) {
        for (PerfilUsuario perfil : permitidos) {
            if (usuario.getPerfil() == perfil) return;
        }
        throw new SecurityException("Você não tem permissão para executar esta ação.");
    }
}
