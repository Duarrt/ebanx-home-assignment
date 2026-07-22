# EBANX Home Assignment — API de Eventos

Uma pequena API bancária que mantém saldos de contas em memória e processa três
tipos de eventos: **depósito** (`deposit`), **saque** (`withdraw`) e
**transferência** (`transfer`). Os saldos ficam em memória e podem ser zerados a
qualquer momento pelo endpoint `/reset`.

## Stack

- Java 17
- Spring Boot 3.5.3 (Spring Web)
- Maven (wrapper incluído — não é preciso ter Maven instalado)

## Requisitos

- JDK 17+ disponível no `PATH` (ou `JAVA_HOME` apontando para ele)

Você **não** precisa do Maven instalado — use o wrapper (`mvnw` / `mvnw.cmd`).

## Como rodar a aplicação

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows (PowerShell / CMD)
mvnw.cmd spring-boot:run
```

O serviço sobe em **http://localhost:8080**.

Para gerar um jar executável:

```bash
./mvnw clean package
java -jar target/ebanxHomeAssignment-0.0.1-SNAPSHOT.jar
```

## Como rodar os testes

```bash
./mvnw test
```

A suíte tem duas camadas: testes **unitários** do `AccountService` (regra de
negócio e mudança real de saldo, sem subir o Spring) e testes de **integração**
do `AccountController` (`@SpringBootTest` + `MockMvc`), cobrindo cada evento, os
endpoints de saldo e reset, os casos de erro `404`/`422` e o parsing
case-insensitive do tipo de evento.

## Referência da API

URL base: `http://localhost:8080`

### `POST /reset`

Zera todo o estado. Sempre retorna `200 OK` com o corpo em texto puro `OK`.

```bash
curl -i -X POST http://localhost:8080/reset
```

### `GET /balance?account_id={id}`

| Caso | Status | Corpo |
|------|--------|-------|
| Conta existe | `200 OK` | o saldo, ex.: `20` |
| Conta não existe | `404 Not Found` | `0` |

```bash
curl -i "http://localhost:8080/balance?account_id=100"
```

### `POST /event`

Endpoint único para os três tipos de evento. O campo `type` é
**case-insensitive** (`deposit`, `DEPOSIT` e `Deposit` funcionam igualmente).

#### Depósito — `type: "deposit"`

Cria a conta de destino se ela não existir e soma `amount` ao saldo.

```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit","destination":"100","amount":10}'
```

`201 Created`
```json
{ "destination": { "id": "100", "balance": 10 } }
```

#### Saque — `type: "withdraw"`

| Caso | Status | Corpo |
|------|--------|-------|
| Sucesso | `201 Created` | `{ "origin": { "id": "100", "balance": 15 } }` |
| Conta de origem não existe | `404 Not Found` | `0` |
| Saldo insuficiente | `422 Unprocessable Entity` | `0` |

```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"withdraw","origin":"100","amount":5}'
```

#### Transferência — `type: "transfer"`

Move `amount` da `origin` para a `destination`. A operação é atômica — se o saque
falha, nenhum valor é movido e a conta de destino não é criada.

| Caso | Status | Corpo |
|------|--------|-------|
| Sucesso | `201 Created` | `{ "origin": {...}, "destination": {...} }` |
| Conta de origem não existe | `404 Not Found` | `0` |
| Saldo insuficiente | `422 Unprocessable Entity` | `0` |

```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"transfer","origin":"100","destination":"300","amount":15}'
```

`201 Created`
```json
{
  "origin":      { "id": "100", "balance": 0  },
  "destination": { "id": "300", "balance": 15 }
}
```

## Decisões principais

- **Armazenamento em memória** — as contas ficam em um `ConcurrentHashMap`, e o
  `/reset` o limpa. Nenhum banco de dados é necessário para o desafio.
- **DTOs de resposta tipados** — as respostas são `record`s Java
  (`DepositResponse`, `WithdrawResponse`, `TransferResponse`) em vez de mapas
  crus, deixando o contrato JSON explícito e type-safe.
- **Separação de camadas** — o `AccountService` contém apenas regra de negócio
  (não conhece HTTP) e lança exceções de domínio; o `AccountController` cuida do
  HTTP (monta os DTOs e mapeia as exceções para os status).
- **`404` vs `422`** — conta inexistente é `404 Not Found`; uma requisição válida
  que viola uma regra de negócio (saldo insuficiente) é `422 Unprocessable
  Entity`. As duas falhas são deliberadamente distintas.
- **Tipo de evento case-insensitive** — o `EventType` usa um `@JsonCreator` do
  Jackson, então a API aceita `"deposit"`, `"DEPOSIT"` etc.; um tipo desconhecido
  resulta em `400 Bad Request`.

## Exemplo de sessão

Reproduz o script de aceitação:

```bash
curl -X POST http://localhost:8080/reset                                            # 200 OK
curl "http://localhost:8080/balance?account_id=1234"                                 # 404 0
curl -X POST http://localhost:8080/event -H "Content-Type: application/json" \
     -d '{"type":"deposit","destination":"100","amount":10}'                         # 201 {"destination":{"id":"100","balance":10}}
curl "http://localhost:8080/balance?account_id=100"                                  # 200 10
curl -X POST http://localhost:8080/event -H "Content-Type: application/json" \
     -d '{"type":"withdraw","origin":"100","amount":5}'                              # 201 {"origin":{"id":"100","balance":5}}
curl -X POST http://localhost:8080/event -H "Content-Type: application/json" \
     -d '{"type":"transfer","origin":"100","destination":"300","amount":5}'         # 201 {"origin":{"id":"100","balance":0},"destination":{"id":"300","balance":5}}
```
