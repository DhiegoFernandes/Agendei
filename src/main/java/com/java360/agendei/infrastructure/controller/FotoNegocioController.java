package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.FotoNegocioService;
import com.java360.agendei.domain.entity.FotoNegocio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/negocios")
@RequiredArgsConstructor
public class FotoNegocioController {

    private final FotoNegocioService fotoNegocioService;

    // Upload de foto
    @PostMapping("/{id}/fotos")
    public ResponseEntity<String> uploadFoto(@PathVariable Integer id,
                                             @RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        fotoNegocioService.adicionarFotoAoNegocio(id, arquivo);
        return ResponseEntity.ok("Foto adicionada com sucesso.");
    }

    // Listar fotos
    @GetMapping("/{id}/fotos")
    public ResponseEntity<List<FotoNegocio>> listarFotos(@PathVariable Integer id) {
        List<FotoNegocio> fotos = fotoNegocioService.listarFotosDoNegocio(id);
        return ResponseEntity.ok(fotos);
    }

    // Acessar foto individual
    @GetMapping("/{negocioId}/fotos/{fotoId}")
    public ResponseEntity<byte[]> getFoto(@PathVariable Integer negocioId,
                                          @PathVariable Integer fotoId) {
        FotoNegocio foto = fotoNegocioService.listarFotosDoNegocio(negocioId).stream()
                .filter(f -> f.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Foto não encontrada"));

        String contentType = foto.getNomeArquivo().endsWith(".png") ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + foto.getNomeArquivo() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(foto.getImagem());
    }
}
