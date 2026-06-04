# RachaConta API

API REST para divisão de despesas entre grupos de amigos.

## 🚀 Deploy

API disponível em produção: https://rachacontaapi-1.onrender.com

Swagger UI: https://rachacontaapi-1.onrender.com/swagger-ui.html

> O plano gratuito do Render hiberna após inatividade. A primeira requisição pode demorar ~30 segundos.

## Tecnologias

- Java 21
- Spring Boot 3.5
- PostgreSQL 16
- Flyway
- Spring Security + JWT
- Docker

## Como rodar localmente

### Pré-requisitos
- Java 21
- Maven
- Docker Desktop

### 1. Sobe o banco

```bash
docker compose up -d
```

### 2. Configura o application.properties

Cria o arquivo `src/main/resources/application.properties` com:

```properties
spring.application.name=rachaconta-api

spring.datasource.url=jdbc:postgresql://localhost:5435/rachacontaapi
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

spring.flyway.enabled=true

jwt.secret=dev-secret-key-minimo-32-caracteres-aqui
jwt.expiration-ms=86400000

server.port=8001
```

### 3. Cria o banco

```bash
docker exec -it rachaconta-db psql -U postgres -c "CREATE DATABASE rachacontaapi;"
docker exec -it rachaconta-db psql -U postgres -c "ALTER USER postgres PASSWORD 'postgres';"
```

> O segundo comando define a senha do usuário postgres para coincidir com o application.properties.

### 4. Sobe a aplicação

```bash
./mvnw spring-boot:run
```

### 5. Acessa o Swagger

http://localhost:8001/swagger-ui.html

## Fluxo de uso

1. `POST /api/auth/register` — cria usuário
2. `POST /api/auth/login` — obtém token JWT
3. Autoriza no Swagger com `Bearer <token>`
4. `POST /api/groups` — cria grupo
5. `POST /api/groups/{id}/members/{userId}` — adiciona membros (apenas ADMIN)
6. `POST /api/groups/{id}/expenses` — registra despesa
7. `POST /api/groups/{id}/settlements/calculate` — calcula quem deve quanto
8. `PATCH /api/groups/{id}/settlements/{settlementId}/confirm` — confirma pagamento
