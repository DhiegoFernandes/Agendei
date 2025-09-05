package com.java360.agendei.infrastructure.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${emailjs.service-id}")
    private String serviceId;

    @Value("${emailjs.template-id}")
    private String templateId;

    @Value("${emailjs.public-key}")
    private String publicKey;

    public void enviarEmailRecuperacao(String email, String nome, String linkRecuperacao) {
        String url = "https://api.emailjs.com/api/v1.0/email/send";

        // Corpo da requisição
        Map<String, Object> body = new HashMap<>();
        body.put("service_id", serviceId);
        body.put("template_id", templateId);
        body.put("user_id", publicKey);

        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("to_email", email);
        templateParams.put("to_name", nome);
        templateParams.put("link", linkRecuperacao);

        body.put("template_params", templateParams);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }
}
