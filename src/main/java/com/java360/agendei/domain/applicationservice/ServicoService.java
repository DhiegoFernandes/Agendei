package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.entity.Disponibilidade;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.model.AgendamentoStatus;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.DisponibilidadeRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.HorariosDisponiveisDTO;
import com.java360.agendei.infrastructure.dto.HorariosPorDiaDTO;
import com.java360.agendei.infrastructure.dto.SaveServicoDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public Servico cadastrarServico(SaveServicoDTO dto) {
        boolean existeMesmoTitulo = servicoRepository
                .existsByTituloAndPrestadorId(dto.getTitulo(), dto.getPrestadorId());
        if (existeMesmoTitulo) {
            throw new IllegalArgumentException("Este prestador já possui um serviço com esse título.");
        }

        Prestador prestador = (Prestador) usuarioRepository.findById(dto.getPrestadorId())
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        Servico servico = Servico.builder()
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .categoria(dto.getCategoria())
                .valor(dto.getValor())
                .duracaoMinutos(dto.getDuracaoMinutos())
                .ativo(true)
                .prestador(prestador)
                .build();

        return servicoRepository.save(servico);
    }

    public List<Servico> listarServicosAtivos() {
        return servicoRepository.findAllByAtivoTrue();
    }

    public HorariosDisponiveisDTO listarHorariosPorServico(String servicoId) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!servico.isAtivo()) {
            throw new IllegalArgumentException("Serviço está desativado.");
        }

        String prestadorId = servico.getPrestador().getId();
        int duracao = servico.getDuracaoMinutos();

        List<Disponibilidade> disponibilidades = disponibilidadeRepository.findByPrestadorId(prestadorId);

        List<Agendamento> agendamentosOcupados = agendamentoRepository.findByServico_Prestador_IdAndStatusIn(
                prestadorId,
                List.of(AgendamentoStatus.PENDING, AgendamentoStatus.CONCLUIDO)
        );

        // Mapeia horários ocupados por dia e hora
        Set<String> horariosOcupados = agendamentosOcupados.stream()
                .map(a -> a.getDataHora().getDayOfWeek().name() + "-" + a.getDataHora().toLocalTime().toString())
                .collect(Collectors.toSet());

        List<HorariosPorDiaDTO> dias = new ArrayList<>();

        for (Disponibilidade d : disponibilidades) {
            List<String> horarios = new ArrayList<>();
            LocalTime hora = d.getHoraInicio();

            while (hora.plusMinutes(duracao).isBefore(d.getHoraFim().plusSeconds(1))) {
                String chave = d.getDiaSemana().name() + "-" + hora.toString();
                if (!horariosOcupados.contains(chave)) {
                    horarios.add(hora.toString());
                }
                hora = hora.plusMinutes(duracao);
            }

            if (!horarios.isEmpty()) {
                dias.add(new HorariosPorDiaDTO(d.getDiaSemana(), horarios));
            }
        }

        return new HorariosDisponiveisDTO(servicoId, dias);
    }

    @Transactional
    public Servico atualizarServico(String id, SaveServicoDTO dto) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        // Verifica duplicação de título para o mesmo prestador, ignorando o próprio serviço atual
        boolean tituloDuplicado = servicoRepository
                .existsByTituloAndPrestadorIdAndIdNot(dto.getTitulo(), dto.getPrestadorId(), id);

        if (tituloDuplicado) {
            throw new IllegalArgumentException("Já existe outro serviço com esse título.");
        }

        servico.setTitulo(dto.getTitulo());
        servico.setDescricao(dto.getDescricao());
        servico.setCategoria(dto.getCategoria());
        servico.setValor(dto.getValor());
        servico.setDuracaoMinutos(dto.getDuracaoMinutos());
        servico.setAtivo(dto.getAtivo());

        return servico;
    }

    @Transactional
    public void desativarServico(String id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        servico.setAtivo(false);
    }
}