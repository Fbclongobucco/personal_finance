# Personal Finance App

Aplicação para controle de finanças pessoais: cadastro de usuários, categorias de receita/despesa e transações, com cálculo automático de saldo.

## Status do projeto

🚧 Em desenvolvimento. Até o momento estão implementados o **modelo de domínio**, as **validações de negócio** e os **contratos de repositório**. A API REST, a camada de persistência (JPA) e a autenticação ainda não existem.

## Stack

- Java 25
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Mail, Flyway)
- PostgreSQL (produção) / H2 (testes/dev)
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

## Como rodar

```bash
./mvnw spring-boot:run
```

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
