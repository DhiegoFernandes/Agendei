package com.java360.agendei.domain.exception;

import com.java360.agendei.infrastructure.exception.RequestException;

public class InvalidAgendamentoStatusException extends RequestException {

    public InvalidAgendamentoStatusException(String statusStr) {
        super("InvalidAgendamentoStatus", "Status de agendamento inválido: " + statusStr);
    }
}
