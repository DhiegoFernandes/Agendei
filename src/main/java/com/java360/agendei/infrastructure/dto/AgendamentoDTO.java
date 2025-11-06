package com.java360.agendei.infrastructure.dto;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.model.StatusAgendamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class AgendamentoDTO {

    private Integer id;
    private String clienteNome;
    private String prestadorNome;
    private String servicoTitulo;
    private Integer servicoId;
    private String enderecoNegocio;
    private String numeroNegocio;
    private String nomeNegocio;
    private LocalDateTime dataHora;
    private StatusAgendamento status;

    public static AgendamentoDTO fromEntity(Agendamento agendamento) {
        String enderecoNegocio = null;
        String numeroNegocio = null;
        String nomeNegocio = null;


        // Evita nullpointer exception
        if (agendamento.getServico() != null && agendamento.getServico().getNegocio() != null) {
            Negocio negocio = agendamento.getServico().getNegocio();
            enderecoNegocio = negocio.getEndereco() != null
                    ? negocio.getEndereco().toString()
                    : null;
            numeroNegocio = negocio.getNumero();
            nomeNegocio = negocio.getNome();
        }

        return new AgendamentoDTO(
                agendamento.getId(),
                agendamento.getCliente().getNome(),
                agendamento.getPrestador().getNome(),
                agendamento.getServico().getTitulo(),
                agendamento.getServico().getId(),
                enderecoNegocio,
                numeroNegocio,
                nomeNegocio,
                agendamento.getDataHora(),
                agendamento.getStatus()
        );
    }
}

