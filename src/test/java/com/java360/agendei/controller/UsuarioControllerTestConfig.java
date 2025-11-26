package com.java360.agendei.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.java360.agendei.infrastructure.security.JwtService;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class UsuarioControllerTestConfig {

    @Bean
    @Primary
    JwtService jwtService() {
        return mock(JwtService.class); // Mocka o serviço para evitar o erro
    }
}
