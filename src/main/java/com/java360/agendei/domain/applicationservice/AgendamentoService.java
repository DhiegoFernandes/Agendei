package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.exception.AgendamentoNotFoundException;
import com.java360.agendei.domain.exception.InvalidAgendamentoStatusException;
import com.java360.agendei.domain.model.AgendamentoStatus;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.infrastructure.dto.SaveAgendamentoDataDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j //Anotação para chamar o logger, se necessario
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;

    //Cria projeto com dados do saveAgendamentoDataDTO
    @Transactional // diz pra jpa que isso é uma transação, usa quando o banco de dados é alterado
    public Agendamento createAgendamento(SaveAgendamentoDataDTO saveAgendamentoData) {
        Agendamento agendamento = Agendamento
                .builder()
                .name(saveAgendamentoData.getName())
                .description(saveAgendamentoData.getDescription())
                .initialDate(saveAgendamentoData.getInitialDate())
                .finalDate(saveAgendamentoData.getFinalDate())
                .status(AgendamentoStatus.PENDING) //Por padrão o agendamento é criado pendente
                .build();

        agendamentoRepository.save(agendamento); //salva no banco de dados

        log.info("Agendamento criado: {}", agendamento); // log pode ser info, error, debug
        System.out.println("Agendamento criado: "+ agendamento);

        return agendamento;
    }

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


}
