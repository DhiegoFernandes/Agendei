package com.java360.agendei.infrastructure.controller;

import com.java360.agendei.domain.applicationservice.FotoNegocioService;
import com.java360.agendei.domain.entity.FotoNegocio;
import com.java360.agendei.domain.repository.FotoNegocioRepository;
import com.java360.agendei.infrastructure.dto.negocio.FotoNegocioDTO;
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
    private final FotoNegocioRepository fotoNegocioRepository;

    // Upload de foto
    @PostMapping("/{id}/fotos")
    public ResponseEntity<String> uploadFoto(@PathVariable Integer id,
                                             @RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        fotoNegocioService.adicionarFotoAoNegocio(id, arquivo);
        return ResponseEntity.ok("Foto adicionada com sucesso.");
    }

    // Listar fotos
    @GetMapping("/{id}/fotos")
    public ResponseEntity<List<FotoNegocioDTO>> listarFotos(@PathVariable Integer id) {
        List<FotoNegocioDTO> fotos = fotoNegocioService.listarFotosDoNegocioDTO(id);
        return ResponseEntity.ok(fotos);
    }

    // Acessar foto individual
    @GetMapping("/{negocioId}/fotos/{fotoId}")
    public ResponseEntity<byte[]> baixarFoto(@PathVariable Integer negocioId,
                                             @PathVariable Integer fotoId) {
        FotoNegocio foto = fotoNegocioRepository.findById(fotoId)
                .orElseThrow(() -> new IllegalArgumentException("Foto não encontrada"));

        if (!foto.getNegocio().getId().equals(negocioId)) {
            throw new IllegalArgumentException("Foto não pertence a este negócio");
        }

        String contentType = foto.getNomeArquivo().endsWith(".png") ?
                MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + foto.getNomeArquivo() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(foto.getImagem());
    }


    @DeleteMapping("/{negocioId}/fotos/{fotoId}")
    public ResponseEntity<String> deletarFoto(@PathVariable Integer negocioId,
                                              @PathVariable Integer fotoId) {
        fotoNegocioService.deletarFoto(negocioId, fotoId);
        return ResponseEntity.ok("Foto deletada com sucesso.");
    }

}
