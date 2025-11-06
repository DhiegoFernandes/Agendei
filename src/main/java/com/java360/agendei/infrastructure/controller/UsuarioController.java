package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.UsuarioService;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PlanoPrestador;
import com.java360.agendei.infrastructure.dto.usuario.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioDTO> registrar(@RequestBody @Valid RegistroUsuarioDTO dto) {
        Usuario usuario = usuarioService.registrarUsuario(dto);
        return ResponseEntity.ok(UsuarioDTO.fromEntity(usuario));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDetalhadoDTO> obterDadosUsuario(@RequestHeader("Authorization") String token) {
        UsuarioDetalhadoDTO usuario = usuarioService.buscarDadosUsuarioPorToken(token);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/todos")
    public ResponseEntity<Page<UsuarioDetalhadoDTO>> listarUsuariosComFiltros(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) com.java360.agendei.domain.model.PerfilUsuario perfil,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefone
    ) {
        Page<UsuarioDetalhadoDTO> usuarios = usuarioService.listarUsuariosComFiltros(
                token, page, size, sortBy, direction, perfil, nome, email, telefone
        );
        return ResponseEntity.ok(usuarios);
    }

    //Atualiza foto perfil
    @PutMapping(value = "/foto-perfil", consumes = "multipart/form-data")
    public ResponseEntity<String> atualizarFotoPerfil(@RequestParam("arquivo") MultipartFile arquivo) {
        usuarioService.atualizarFotoPerfil(arquivo);
        return ResponseEntity.ok("Foto de perfil atualizada com sucesso.");
    }


    // pega url da foto perfil
    @GetMapping("/{id}/foto-perfil-url")
    public ResponseEntity<FotoPrestadorDTO> getFotoPerfilUrl(@PathVariable Integer id) {
        FotoPrestadorDTO dto = usuarioService.buscarFotoPerfilDTO(id);
        return ResponseEntity.ok(dto);
    }

    // pega a foto (base64)
    @GetMapping("/{id}/foto-perfil")
    public ResponseEntity<byte[]> getFotoPerfil(@PathVariable Integer id) {
        byte[] imagem = usuarioService.buscarFotoPerfilBytes(id);

        // Detecta tipo de imagem (padrão: JPEG)
        String contentType = "image/jpeg";
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"foto-perfil-" + id + ".jpg\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(imagem);
    }

    @PutMapping("/me/atualizar")
    public ResponseEntity<UsuarioDetalhadoDTO> atualizarDadosPrestador(
            @RequestBody @Valid AtualizarPrestadorDTO dto) {
        UsuarioDetalhadoDTO atualizado = usuarioService.atualizarDadosPrestador(dto);
        return ResponseEntity.ok(atualizado);
    }



}
