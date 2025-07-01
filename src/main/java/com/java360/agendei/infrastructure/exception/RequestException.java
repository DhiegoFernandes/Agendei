package com.java360.agendei.infrastructure.exception;

import lombok.Getter;

@Getter
public class RequestException extends RuntimeException{

    private final String errorCode;

    public RequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
