# Personal Finance App

Aplicação para controle de finanças pessoais: cadastro de usuários, categorias de receita/despesa e transações, com cálculo automático de saldo.

## Status do projeto

🚧 Em desenvolvimento, com o fluxo principal funcional de ponta a ponta.

Implementado:

- **Domínio e regras de negócio** — `User`, `Category` e `Transaction`, com validação de invariantes e saldo derivado.
- **Persistência** — entidades JPA e adapters que implementam os contratos do `core`; schema versionado em Flyway.
- **API REST** — `/auth` (registro, login, refresh) e `/api/users`, `/api/categories`, `/api/transactions`, com tratamento centralizado de erro.
- **Autenticação e autorização** — Spring Security com JWT (access e refresh token), senha em BCrypt, `AccessGuard` restringindo cada recurso ao seu dono, e bootstrap do usuário administrador.
- **Documentação da API** — OpenAPI/Swagger UI em `/swagger-ui.html`.
- **Infraestrutura** — imagem publicada no Docker Hub pelo pipeline de CD, banco na Neon e nginx como proxy reverso.

Testes cobrem domínio, mappers, casos de uso e os controllers (integração via MockMvc).

## Stack

- Java 25
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Mail, Flyway)
- PostgreSQL serverless na Neon (produção) / H2 em memória (dev e testes)
- Maven
- JUnit 5

## Arquitetura

O projeto segue uma organização inspirada em Clean Architecture: o pacote `core` concentra as regras de negócio, isolado de frameworks e de detalhes de infraestrutura.

```
core
├── domain       # entidades de negócio (User, Category, Transaction)
├── exception    # exceptions de validação de domínio
└── repository   # contratos de persistência (interfaces, sem JPA)
```

- **`core.domain`** — entidades imutáveis (exceto o saldo, que é estado derivado), criadas apenas por factory methods (`createUser`, `createCategory`, `Transaction.create`, etc.) que validam suas próprias invariantes.
- **`core.exception`** — uma exception por recurso (`InvalidUserException`, `InvalidCategoryException`, `InvalidTransactionException`), todas estendendo `DomainException`.
- **`core.repository`** — interfaces (`UserRepository`, `CategoryRepository`, `TransactionRepository`) que descrevem os contratos de persistência; as implementações vivem em `infra.rest.adapters`, sobre os repositórios JPA.

### Modelo de domínio

- **User** — dono das transações. Mantém `initialBalance` e um saldo (`balance`) recalculado automaticamente a cada transação adicionada. Valida nome, e-mail (formato), telefone (10 ou 11 dígitos) e senha.
- **Category** — classifica uma transação como `INCOME` ou `EXPENSE`. O próprio enum `Type` sabe como aplicar seu efeito no saldo (`apply(balance, amount)`).
- **Transaction** — sempre vinculada a um `User` e a uma `Category`; ao ser criada (`create`/`recover`), já se registra automaticamente no usuário, mantendo o saldo sempre consistente. Exige valor positivo, descrição, categoria, usuário e forma de pagamento.

## Banco de dados

A aplicação usa dois profiles:

- **`h2`** (padrão) — H2 em memória, para desenvolvimento e testes, sem configuração externa.
- **`postgres`** — Neon Postgres, usado em produção.

O schema é versionado em `src/main/resources/db/migration` e aplicado pelo Flyway na subida da aplicação.

### Configuração da Neon

No painel da Neon, em **Connection Details**, selecione o formato **JDBC** e copie os dados da branch desejada. Use o host com sufixo `-pooler` (PgBouncer) e mantenha os parâmetros de TLS na URL.

Variáveis consumidas pelo profile `postgres` (sem valor padrão — a aplicação não sobe se faltarem):

```
APP_DB_URL         jdbc:postgresql://<host>-pooler.<regiao>.aws.neon.tech/<database>?sslmode=require&channelBinding=require
APP_DB_USERNAME    usuário do role da Neon (ex.: neondb_owner)
APP_DB_PASSWORD    senha do role
APP_JWT_SECRET     segredo de assinatura do JWT
APP_ADMIN_EMAIL    e-mail do administrador criado no bootstrap (padrão: admin@personalfinance.local)
APP_ADMIN_PASSWORD senha do administrador (padrão: admin123)
```

O compute da Neon suspende em ociosidade e encerra conexões paradas. O pool HikariCP já está dimensionado para isso em `application-postgres.yaml` (`maximum-pool-size: 5`, `minimum-idle: 0`, `max-lifetime: 5 min`); não aumente o pool sem verificar o limite de conexões do plano contratado.

## Como rodar

### Local, com H2 (padrão)

```bash
./mvnw spring-boot:run
```

### Local, contra a Neon

```bash
export SPRING_PROFILES_ACTIVE=postgres
export APP_DB_URL='jdbc:postgresql://<host>-pooler.<regiao>.aws.neon.tech/<database>?sslmode=require&channelBinding=require'
export APP_DB_USERNAME='<usuario>'
export APP_DB_PASSWORD='<senha>'
export APP_JWT_SECRET='<segredo>'
./mvnw spring-boot:run
```

### Docker Compose

O `docker-compose.yml` sobe apenas a aplicação — não há mais Postgres em container. O banco é a Neon.

```bash
cp .env.example .env   # preencha APP_DB_*, APP_JWT_SECRET
docker compose up -d
```

O `.env` está no `.gitignore` e nunca deve ser versionado. As variáveis obrigatórias são validadas na interpolação: se alguma estiver vazia, o `docker compose up` falha antes de iniciar o container.

A aplicação responde em `http://localhost:8080`.

## Como testar

```bash
./mvnw test
```

Os testes cobrem as validações de domínio de `User`, `Category` e `Transaction` (bloqueio de dados inválidos e cálculo correto de saldo).

## Próximos passos

- **Executar os testes no pipeline** — o workflow de CD roda `package -DskipTests`, então nenhuma regressão é barrada antes da publicação da imagem.
- **Testar contra Postgres** — os testes de integração sobem no profile `h2`; divergências de dialeto e de migration só aparecem em produção. Avaliar Testcontainers ou uma branch descartável da Neon.
- **Paginar as listagens** — `GET /api/transactions` e `GET /api/categories` retornam a coleção inteira, sem `Pageable` nem filtro por período.
- **Tratar o ciclo de vida do refresh token** — hoje é stateless: não há persistência, rotação nem revogação, e o logout não invalida token emitido.
- **Definir o uso de e-mail** — `spring-boot-starter-mail` está declarado no `pom.xml` sem nenhum uso; implementar o fluxo pretendido (recuperação de senha) ou remover a dependência.
- **Observabilidade** — sem Actuator, health check ou métricas expostas para o nginx e para o monitoramento do container.
