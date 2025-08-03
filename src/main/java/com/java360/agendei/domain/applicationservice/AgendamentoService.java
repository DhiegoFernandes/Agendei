package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.CreateAgendamentoDTO;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeService disponibilidadeService;

    @Transactional
    public Agendamento criarAgendamento(CreateAgendamentoDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.CLIENTE);

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        Prestador prestador = servico.getPrestador();
        LocalDateTime inicio = dto.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());

        boolean disponivel = disponibilidadeService.prestadorEstaDisponivel(prestador.getId(), inicio, servico.getDuracaoMinutos());
        if (!disponivel) {
            throw new IllegalArgumentException("O prestador não está disponível nesse horário.");
        }

        if (!prestador.getNegocio().isAtivo()) {
            throw new IllegalArgumentException("Negócio está inativo.");
        }

        // Impede agendamentos duplicados no mesmo horário
        boolean ocupado = agendamentoRepository.existsByPrestadorIdAndDataHoraBetween(prestador.getId(), inicio, fim.minusMinutes(1));
        if (ocupado) {
            throw new IllegalArgumentException("Horário indisponível.");
        }

        Agendamento agendamento = Agendamento.builder()
                .cliente((Cliente) usuario)
                .servico(servico)
                .prestador(prestador)
                .dataHora(dto.getDataHora())
                .status(StatusAgendamento.PENDENTE)
                .build();

        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void concluirAgendamento(Integer agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));

        Usuario usuario = UsuarioAutenticado.get();
        if (!agendamento.getPrestador().getId().equals(usuario.getId()) &&
                !PermissaoUtils.isAdmin(usuario)) {
            throw new SecurityException("Sem permissão para concluir este agendamento.");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
    }
}

