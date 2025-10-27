package com.java360.agendei.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // habilita CORS
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**") // Permite POST no H2
                        .disable()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable()) // Permite exibição do H2 em frame
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/h2-console/**",
                                "/auth/**",
                                "/usuarios/registrar",
                                "/servicos/ativos",
                                "/servicos/*/horarios-disponiveis-data",
                                "/servicos/busca",
                                "/servicos/negocio/**",
                                "/negocios/*/fotos/**",
                                "/negocios/**"
                        ).permitAll()// Acesso livre nessas URI
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Custo 10 (padrão)
    }

    // Necessário se quiser usar AuthenticationManager diretamente
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // CORS para múltiplos ambientes
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Todas as URLs que precisam acessar a API
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",   // desenvolvimento local

                //TODO
                "https://staging.agendei.com.br", // staging
                "https://www.agendei.com.br"      // produção
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*")); // inclui Authorization, Content-Type etc.
        configuration.setAllowCredentials(true); // permite envio de token/cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // aplica a todas as rotas
        return source;
    }
}
