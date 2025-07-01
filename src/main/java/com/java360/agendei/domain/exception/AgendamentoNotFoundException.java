package com.java360.agendei.domain.exception;

import com.java360.agendei.infrastructure.exception.RequestException;

public class AgendamentoNotFoundException extends RequestException {

    public AgendamentoNotFoundException(String agendamentoId) {
        super("AgendamentoNotFound", "Agendamento não encontrado: " + agendamentoId);
    }
}
