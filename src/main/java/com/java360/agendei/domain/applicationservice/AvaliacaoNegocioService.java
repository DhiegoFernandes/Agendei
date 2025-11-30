package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.AvaliacaoNegocioRepository;
import com.java360.agendei.infrastructure.dto.negocio.AvaliacaoNegocioDTO;
import com.java360.agendei.infrastructure.dto.negocio.CreateAvaliacaoNegocioDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvaliacaoNegocioService {

    private final AvaliacaoNegocioRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public AvaliacaoNegocioDTO criarAvaliacao(CreateAvaliacaoNegocioDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.CLIENTE);

        Cliente cliente = (Cliente) usuario;

        Agendamento agendamento = agendamentoRepository.findById(dto.getAgendamentoId())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        if (!agendamento.getCliente().getId().equals(cliente.getId())) {
            throw new SecurityException("Você não pode avaliar um agendamento que não é seu.");
        }

        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) {
            throw new IllegalArgumentException("Só é possível avaliar após a conclusão do agendamento.");
        }

        if (dto.getNota() < 1 || dto.getNota() > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
        }



        // Verifica se o cliente já avaliou o negócio
        AvaliacaoNegocio avaliacaoExistente = avaliacaoRepository
                .findByNegocioIdAndClienteId(
                        agendamento.getPrestador().getNegocio().getId(),
                        cliente.getId()
                )
                .orElse(null);

        AvaliacaoNegocio avaliacao;
        if (avaliacaoExistente != null) {
            // Atualiza avaliação existente
            avaliacaoExistente.setNota(dto.getNota());
            avaliacaoExistente.setComentario(dto.getComentario());
            avaliacaoExistente.setDataAvaliacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
            avaliacao = avaliacaoExistente;
        } else {
            // Cria nova avaliação
            avaliacao = AvaliacaoNegocio.builder()
                    .negocio(agendamento.getPrestador().getNegocio())
                    .cliente(cliente)
                    .agendamento(agendamento)
                    .nota(dto.getNota())
                    .comentario(dto.getComentario())
                    .dataAvaliacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                    .build();
        }

        AvaliacaoNegocio salva = avaliacaoRepository.save(avaliacao);

        atualizarMediaNegocio(salva.getNegocio());


        // Retorna avaliação criada
        return AvaliacaoNegocioDTO.builder()
                .id(salva.getId())
                .negocioId(salva.getNegocio().getId())
                .nomeNegocio(salva.getNegocio().getNome())
                .clienteId(salva.getCliente().getId())
                .nomeCliente(salva.getCliente().getNome())
                .nota(salva.getNota())
                .comentario(salva.getComentario())
                .dataAvaliacao(salva.getDataAvaliacao())
                .build();
    }

    // Retorna avaliações do negócio
    public List<AvaliacaoNegocioDTO> listarAvaliacoesNegocio(Integer negocioId) {
        return avaliacaoRepository.findByNegocioId(negocioId)
                .stream()
                .map(av -> AvaliacaoNegocioDTO.builder()
                        .id(av.getId())
                        .negocioId(av.getNegocio().getId())
                        .nomeNegocio(av.getNegocio().getNome())
                        .clienteId(av.getCliente().getId())
                        .nomeCliente(av.getCliente().getNome())
                        .nota(av.getNota())
                        .comentario(av.getComentario())
                        .dataAvaliacao(av.getDataAvaliacao())
                        .build()
                ).toList();
    }

    private void atualizarMediaNegocio(Negocio negocio) {
        List<AvaliacaoNegocio> avaliacoes = avaliacaoRepository.findByNegocioId(negocio.getId());
        double media = avaliacoes.stream().mapToInt(AvaliacaoNegocio::getNota).average().orElse(0);
        negocio.setNotaMedia(media);
    }
}