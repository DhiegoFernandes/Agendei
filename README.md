# Agendei - Backend

Versão: v1.0.0
Último Commit: <INSIRA_AQUI_O_HASH_DO_ULTIMO_COMMIT>
Data: 30/11/2025

# Descrição do Projeto:
O Agendei é um sistema de agendamento de serviços online, voltado para pequenos negócios e profissionais de estética e beleza. Permite que clientes agendem serviços, visualizem horários disponíveis, e que prestadores configurem sua disponibilidade, horário de almoço, e gerenciem seus agendamentos.

O sistema foi desenvolvido em Spring Boot 3.4.4 com arquitetura em camadas, utilizando JPA/Hibernate para persistência de dados e Spring Security com JWT para autenticação.

# Tecnologias e Frameworks:
- Java: 21
- Spring Boot: 3.4.4
- Spring Data JPA: Persistência de dados
- Spring Security + JWT: Autenticação e autorização
- Spring Boot Starter Mail: Envio de e-mails
- Banco de Dados:
    - SQL Server (Azure) - Produção
    - MySQL - Desenvolvimento local
    - H2 (Memória) - Desenvolvimento local
- Lombok: Redução de boilerplate
- Google Maps API: Para geolocalização de negócios
- JUnit + Spring Boot Test: Testes unitários e de integração
- JaCoCo: Cobertura de testes
- Logging: commons-logging 1.2

# Estrutura de Perfis:
Perfil        | Banco de Dados          | Uso
------------ | ---------------------- | --------------------------------
dev          | H2 em memória           | Testes e desenvolvimento local
localmysql   | MySQL localhost         | Desenvolvimento local
sqlserver    | SQL Server Azure        | Produção alternativa

Configuração e Deployment:

1. Clone do Repositório:
git clone https://github.com/dhiegosenac/agendeiFrontSenac.git
cd agendei

2. Instalar dependências e build:
mvn clean install

3. Rodar o backend:
# Para perfil de desenvolvimento (H2)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Para SQL Server
mvn spring-boot:run -Dspring-boot.run.profiles=sqlserver

# Para MySQL produção
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Banco de Dados:

Script de criação (MySQL exemplo local):
CREATE DATABASE agendei;
USE agendei;
-- Tabelas serão criadas automaticamente pelo Hibernate

Observação: Para produção, configure ddl-auto: none ou use scripts específicos fornecidos.

Testes:
- Cobertura de testes com JaCoCo
- Executar todos os testes:
mvn test

Autenticação:
- JWT com Spring Security
- Roles: CLIENTE, PRESTADOR, ADMIN
- O ADMIN possui permissão total no sistema

Projeto agendei:
- Desenvolvedores: Dhiego Fernandes da Silva, Guilherme Cunha Alves, Moises Alves Silva
- Frontend integrado: https://red-island-067d14e0f.3.azurestaticapps.net
