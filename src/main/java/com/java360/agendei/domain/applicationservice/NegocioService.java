package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.repository.NegocioRepository;
import com.java360.agendei.domain.repository.PrestadorRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.ConviteNegocioDTO;
import com.java360.agendei.infrastructure.dto.CreateNegocioDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NegocioService {
    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;
    private final PrestadorRepository prestadorRepository;

    @Transactional
    public Negocio criarNegocio(CreateNegocioDTO dto) {
        if (negocioRepository.existsByNome(dto.getNome())) {
            throw new IllegalArgumentException("Nome do negócio já está em uso.");
        }

        Prestador prestador = (Prestador) usuarioRepository.findById(dto.getPrestadorId())
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        if (prestador.getNegocio() != null) {
            throw new IllegalArgumentException("Você já está vinculado a um negócio. Saia do atual antes de criar outro.");
        }

        Negocio negocio = Negocio.builder()
                .nome(dto.getNome())
                .endereco(dto.getEndereco())
                .criador(prestador)
                .build();

        Negocio criado = negocioRepository.save(negocio);

        // Associa o criador ao negócio
        prestador.setNegocio(criado);

        return criado;
    }

    @Transactional
    public void convidarPrestadorParaNegocio(ConviteNegocioDTO dto) {
        Negocio negocio = negocioRepository.findById(dto.getNegocioId())
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmailPrestador())
                .orElseThrow(() -> new IllegalArgumentException("Prestador com esse e-mail não existe."));

        if (!(usuario instanceof Prestador prestador)) {
            throw new IllegalArgumentException("Usuário não é um prestador.");
        }
        if (!negocio.getCriador().getId().equals(dto.getIdDonoNegocio())) {
            throw new IllegalArgumentException("Apenas o dono do negócio pode convidar prestadores.");
        }
        if (prestador.getNegocio() != null) {
            throw new IllegalArgumentException("Prestador já está vinculado a outro negócio.");
        }



        // Associa o prestador ao negócio
        prestador.setNegocio(negocio);
    }

    @Transactional
    public void sairDoNegocio(String prestadorId) {
        Prestador prestador = (Prestador) usuarioRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        Negocio negocio = prestador.getNegocio();
        if (negocio == null) {
            throw new IllegalArgumentException("Prestador não está associado a nenhum negócio.");
        }

        if (negocio.getCriador().getId().equals(prestador.getId())) {
            throw new IllegalArgumentException("O dono do negócio não pode se desvincular.");
        }

        // Desativa os serviços ativos desse prestador nesse negócio
        List<Servico> servicos = servicoRepository.findByPrestadorIdAndNegocioId(prestadorId, negocio.getId());
        servicos.forEach(s -> s.setAtivo(false));

        // desvincula o prestador do negócio
        prestador.setNegocio(null);
    }

    @Transactional
    public void excluirNegocio(String negocioId, String solicitanteId) {
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));

        if (!negocio.getCriador().getId().equals(solicitanteId)) {
            throw new IllegalArgumentException("Apenas o dono do negócio pode excluí-lo.");
        }

        negocio.setAtivo(false);

        // Desativa serviços
        List<Servico> servicos = servicoRepository.findByNegocio_IdAndAtivoTrue(negocioId);
        servicos.forEach(s -> s.setAtivo(false));

        // Desvincula prestadores
        List<Prestador> prestadores = prestadorRepository.findByNegocio_Id(negocioId);
        prestadores.forEach(p -> p.setNegocio(null));
    }



}
