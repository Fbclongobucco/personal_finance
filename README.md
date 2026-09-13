# Personal Finance App

Aplicação para controle de finanças pessoais: cadastro de usuários, categorias de receita/despesa e transações, com cálculo automático de saldo.

## Status do projeto

🚧 Em desenvolvimento. Até o momento estão implementados o **modelo de domínio**, as **validações de negócio** e os **contratos de repositório**. A API REST, a camada de persistência (JPA) e a autenticação ainda não existem.

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
- **`core.repository`** — interfaces (`UserRepository`, `CategoryRepository`, `TransactionRepository`) que descrevem os contratos de persistência; as implementações (JPA/adapters) ficam para a camada de infraestrutura, ainda não criada.

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

- Implementar os adapters de persistência (JPA) para os repositórios do `core`
- Camada de aplicação (casos de uso) e API REST
- Autenticação/autorização (Spring Security)
- Migrations com Flyway
