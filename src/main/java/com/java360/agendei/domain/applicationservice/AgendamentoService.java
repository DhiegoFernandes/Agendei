package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.entity.Cliente;
import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.exception.AgendamentoNotFoundException;
import com.java360.agendei.domain.exception.InvalidAgendamentoStatusException;
import com.java360.agendei.domain.model.AgendamentoStatus;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.domain.repository.DisponibilidadeRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.SaveAgendamentoDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j //Anotação para chamar o logger, se necessario
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;
    private final DisponibilidadeService disponibilidadeService;

    @Transactional
    public Agendamento createAgendamento(SaveAgendamentoDTO dto) {
        Cliente cliente = (Cliente) usuarioRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        // Valida se prestador atende nesse horário
        if (!disponibilidadeService.prestadorEstaDisponivel(servico.getPrestador().getId(), dto.getDataHora())) {
            throw new IllegalArgumentException("O prestador não atende neste horário.");
        }

        // Verifica se o horário já está ocupado (pendente ou concluído)
        boolean ocupado = agendamentoRepository.existsByServico_Prestador_IdAndDataHoraAndStatusIn(
                servico.getPrestador().getId(),
                dto.getDataHora(),
                List.of(AgendamentoStatus.PENDING, AgendamentoStatus.CONCLUIDO)
        );

        if (ocupado) {
            throw new IllegalArgumentException("O horário já está ocupado.");
        }

        Agendamento agendamento = Agendamento.builder()
                .cliente(cliente)
                .servico(servico)
                .dataHora(dto.getDataHora())
                .status(AgendamentoStatus.PENDING)
                .build();

        return agendamentoRepository.save(agendamento);
    }

/*
    public Agendamento loadAgendamento(String agendamentoId){
        //retorna o agendamento OU gera exceção
        return agendamentoRepository.
                findById(agendamentoId)
                .orElseThrow(() -> new AgendamentoNotFoundException(agendamentoId)); //Exceção do pacote exception
    }

    @Transactional
    public void deleteAgendamento(String agendamentoId){
        Agendamento agendamento = loadAgendamento(agendamentoId);
        agendamentoRepository.delete(agendamento);
    }

    @Transactional
    public Agendamento updateAgendamento(String agendamentoId, SaveAgendamentoDataDTO saveAgendamentoData){
        Agendamento agendamento = loadAgendamento(agendamentoId); //carrega o agendamento

        agendamento.setName(saveAgendamentoData.getName());
        agendamento.setDescription(saveAgendamentoData.getDescription());
        agendamento.setInitialDate(saveAgendamentoData.getInitialDate());
        agendamento.setFinalDate(saveAgendamentoData.getFinalDate());
        //Converte o status para enum
        agendamento.setStatus(convertToAgendamentoStatus(saveAgendamentoData.getStatus()));

        return agendamento;
    }

    private AgendamentoStatus convertToAgendamentoStatus(String statusStr){
        try {
            return AgendamentoStatus.valueOf(statusStr); // valueOf converte a string no Enum
        } catch (IllegalArgumentException | NullPointerException e){ // Se Enum invalido ou nulo cai na exceção
            throw new InvalidAgendamentoStatusException(statusStr); // chama ex do pacote exception
        }

    }
*/

}
