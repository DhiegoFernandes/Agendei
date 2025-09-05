package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.FotoNegocio;
import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.FotoNegocioRepository;
import com.java360.agendei.domain.repository.NegocioRepository;
import com.java360.agendei.infrastructure.dto.FotoNegocioDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import com.java360.agendei.domain.model.PerfilUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FotoNegocioService {

    private final NegocioRepository negocioRepository;
    private final FotoNegocioRepository fotoNegocioRepository;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Transactional
    public void adicionarFotoAoNegocio(Integer negocioId, MultipartFile arquivo) throws Exception {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));

        if (!negocio.getCriador().getId().equals(usuario.getId()) && !PermissaoUtils.isAdmin(usuario)) {
            throw new IllegalArgumentException("Apenas o dono do negócio pode adicionar fotos.");
        }

        // Validação do arquivo
        validarArquivo(arquivo);

        // Cria e salva a foto
        FotoNegocio foto = FotoNegocio.builder()
                .imagem(arquivo.getBytes())
                .nomeArquivo(arquivo.getOriginalFilename())
                .negocio(negocio)
                .build();

        fotoNegocioRepository.save(foto);
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }

        String contentType = arquivo.getContentType();
        if (!("image/jpeg".equals(contentType) || "image/png".equals(contentType))) {
            throw new IllegalArgumentException("Formato inválido. Apenas JPEG ou PNG são aceitos.");
        }

        if (arquivo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo 5MB.");
        }
    }

    // Mostra o byte inteiro
    @Transactional(readOnly = true)
    public List<FotoNegocio> listarFotosDoNegocio(Integer negocioId) {
        return fotoNegocioRepository.findByNegocioId(negocioId);
    }

    // Mostra apenas o url
    @Transactional(readOnly = true)
    public List<FotoNegocioDTO> listarFotosDoNegocioDTO(Integer negocioId) {
        return fotoNegocioRepository.findByNegocioId(negocioId)
                .stream()
                .map(f -> new FotoNegocioDTO(
                        f.getId(),
                        f.getNomeArquivo(),
                        "/negocios/" + negocioId + "/fotos/" + f.getId() // URL para download
                ))
                .toList();
    }


    @Transactional
    public void deletarFoto(Integer negocioId, Integer fotoId) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        FotoNegocio foto = fotoNegocioRepository.findById(fotoId)
                .orElseThrow(() -> new IllegalArgumentException("Foto não encontrada."));

        Negocio negocio = foto.getNegocio();

        if (!negocio.getId().equals(negocioId)) {
            throw new IllegalArgumentException("Foto não pertence a esse negócio.");
        }

        // Apenas o dono do negócio ou prestador associado pode deletar
        boolean isDono = negocio.getCriador().getId().equals(usuario.getId());
        boolean isPrestadorAssociado = usuario instanceof Prestador p && p.getNegocio() != null && p.getNegocio().getId().equals(negocioId);

        if (!isDono && !isPrestadorAssociado && !PermissaoUtils.isAdmin(usuario)) {
            throw new IllegalArgumentException("Você não tem permissão para deletar esta foto.");
        }

        fotoNegocioRepository.delete(foto);
    }


}
