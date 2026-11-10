# Agent Instructions - Cocos Challenge Backend

API de brokerage para el [backend challenge de Cocos](https://github.com/cocoscap/cocos-challenge/blob/main/backend-challenge.md), en Java en lugar de Node.js. Las decisiones de negocio están en `cocos-action-plan.md` y en el README; si una regla de este archivo choca con ellas, mandan esos documentos.

## 1. Project Overview & Tech Stack

- **Language:** Java 21 (LTS). La máquina puede tener otro JDK por defecto: usar `JAVA_HOME` apuntando a un JDK 21.
- **Framework:** Spring Boot 4.1 (`spring-boot-starter-webmvc`, no `starter-web`).
- **Build Tool:** Maven, siempre con el wrapper `./mvnw`.
- **Database:** PostgreSQL con Spring Data JPA. **Sin Flyway**: el esquema es el del `database.sql` oficial y no se modifica.
- **Tests:** JUnit 5, AssertJ, MockMvc, Testcontainers (Postgres 17). No hay Mockito en uso.
- **Formato:** Spring Java Format (`spring-javaformat-maven-plugin`), verificado en la fase `validate`.
- **No se usan:** Lombok, MapStruct, Spotless, Checkstyle. No agregarlos sin pedirlo.

## 2. Essential Commands

Docker tiene que estar corriendo para todo lo que toca la base (Testcontainers y Compose).

### Build & Verification

- Compilar sin tests: `./mvnw clean compile`
- Suite completa: `./mvnw test`
- Una clase: `./mvnw test -Dtest=ChallengeEndToEndTest`
- Un test: `./mvnw test -Dtest=OrderApiTest#marketBuyFillsAtTheLatestClose`
- Formatear: `./mvnw spring-javaformat:apply`. Verificar sin tocar: `./mvnw spring-javaformat:validate`.

Todo build corre `spring-javaformat:validate` y falla si un archivo no está formateado. Antes de cerrar un cambio, correr `apply`. Tabs, `else`/`catch` en línea nueva, imports agrupados `java.*`, `com.cocos`, `jakarta`, `org` (y estáticos primero). Ninguna línea pasa de 120 columnas y ninguna clase de 400 líneas: si llega, se divide.

### Run Locally

- En la máquina, con Postgres de Testcontainers y el seed: `./mvnw spring-boot:test-run`
- Con Docker Compose y Postgres propio: `docker compose --profile local up --build`
- Contra Neon: `DB_TARGET=neon` y las variables `SPRING_DATASOURCE_*` en `.env`, después `docker compose up --build`

La API queda en `http://localhost:8080`. `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` **no** funciona solo: el perfil `local` espera que `docker/entrypoint.sh` le pase el datasource.

## 3. Architecture & Package Structure

Capas simples, sin puertos ni adaptadores. El challenge es chico y el hexagonal estricto agrega ruido.

```text
src/main/java/com/cocos/challenge/
├── api/
│   ├── controller/             # @RestController
│   ├── dto/request/            # Records de entrada y enums que manda el cliente (TradeSide)
│   ├── dto/response/           # Records de salida con from(domain)
│   ├── validation/             # @ValidPlaceOrder y su ConstraintValidator
│   ├── error/                  # @RestControllerAdvice → ProblemDetail
│   └── filter/                 # Filtros servlet (headers de seguridad)
├── application/
│   ├── service/                # Casos de uso (@Service, @Transactional)
│   ├── command/                # Entrada de los casos de uso (PlaceOrderCommand)
│   ├── mapper/                 # Entidad → record de dominio (OrderMapper)
│   └── support/                # Helpers de consulta (QuoteLookup, LikePatterns)
├── config/                     # @Configuration (Clock, BalanceCalculator)
├── domain/
│   ├── model/                  # Records de negocio
│   ├── enums/
│   ├── service/                # BalanceCalculator, Java puro
│   ├── support/                # MoneyMath: redondeo y porcentajes
│   └── exception/
└── infrastructure/persistence/
    ├── entity/                 # @Entity
    └── repository/             # Spring Data
```

Una clase nueva va al paquete de su tipo, no al de la feature. Un record de respuesta va en `api/dto/response` aunque lo use un solo controller.

### Reglas de Dependencia

1. Los controllers solo llaman a servicios de `application`. Nunca a repositorios.
2. `domain` no importa Spring, Jakarta ni `infrastructure`. Es Java puro y se testea sin contexto.
3. `application` usa los repositorios y las entidades directamente y convierte a records de `domain` antes de devolver, con `OrderMapper` para las órdenes. No hay interfaces de puerto: no crearlas sin un motivo.
4. Las entidades `@Entity` nunca salen por la API, y tampoco los records de `domain`. Los controllers devuelven DTOs de `api/dto/response` (`PortfolioResponse`, `OrderResponse`, `InstrumentResponse`). El mapeo es a mano, en `from(...)`. Renombrar un campo de un DTO rompe el contrato JSON.
5. Los requests son records en `api/dto/request` (`PlaceOrderRequest`). Los enums que el cliente puede mandar son propios de la API (`TradeSide` solo tiene `BUY` y `SELL`).
6. Redondeo de plata y porcentajes solo con `MoneyMath` (2 decimales, `HALF_UP`; divisiones a 8). No repetir `setScale` en los servicios.

## 4. Coding Conventions & Spring Idioms

### Inyección de Dependencias

- Inyección por constructor, escrita a mano. Sin Lombok.
- Prohibido `@Autowired` en atributos del código de producción. En clases de test se acepta.

### Entidades JPA

- Una entidad por tabla, sin relaciones: las claves foráneas se mapean como `Integer` (`instrumentId`, `userId`). No agregar `@ManyToOne` ni `@OneToMany`.
- **Toda columna lleva `@Column(name = ...)` explícito en minúsculas.** Postgres guardó los nombres camelCase del seed sin comillas: `accountnumber`, `instrumentid`, `userid`, `previousclose`. Spring los traduciría a `account_number` y `ddl-auto=validate` falla al arrancar.
- `marketdata` tiene la columna `date` (tipo `DATE`), no `datetime` como dice el enunciado.
- Plata y precios con `BigDecimal`, nunca `double`. Enums con `@Enumerated(EnumType.STRING)`.

### Transaccionalidad

- `@Transactional` va en los métodos públicos de los servicios de `application`. `@Transactional(readOnly = true)` para los de lectura.
- Nunca en controllers ni en métodos privados.
- Crear y cancelar órdenes bloquea primero la fila del usuario (`SELECT … FOR UPDATE`). No sacar ese lock: evita que dos órdenes concurrentes usen el mismo cash.

### Errores y Validación

- `jakarta.validation` en los requests, con `@Valid` en el controller. Las reglas entre campos van en `@ValidPlaceOrder`.
- Desconocer campos extra del JSON es un 400 (`fail-on-unknown-properties=true`).
- `ApiExceptionHandler` devuelve `ProblemDetail` (RFC 9457). Sin stack traces ni mensajes internos en la respuesta.
- `domain` lanza `NotFoundException`, `InvalidOrderException` y `ConflictException`. Nunca tipos HTTP.
- Logs, excepciones y comentarios en inglés. No loguear connection strings ni credenciales.

## 5. Database & Schema

- `spring.jpa.hibernate.ddl-auto=validate` en todos los perfiles. Nunca `update`, `create` ni `create-drop`.
- **El esquema no se modifica.** No hay carpeta `db/migration`. Los índices sugeridos están en `docs/optional-indexes.sql` y no se aplican. Si hace falta un cambio de esquema, primero se acuerda y se documenta en el README.
- El seed vive en `src/test/resources/database.sql`. Es una copia del archivo oficial: no editarlo.
- Neon ya tiene el seed. La aplicación no crea tablas ni recarga datos ahí.

## 6. Reglas de negocio que no se cambian sin acordarlo

- Cash y posiciones se calculan siempre desde `orders` (columna `size`). No hay tabla de posiciones.
- `MARKET` usa el último `close` y queda `FILLED`. Sin precio (PGR, IRCP) queda `REJECTED`.
- `LIMIT` queda `NEW`. No hay matching.
- Por monto: `floor(monto / precio)`. Si da 0 acciones, la orden se guarda `REJECTED`.
- Las órdenes rechazadas se guardan y responden 201 con `status: REJECTED`.
- `POST /orders` solo acepta `BUY` y `SELL`. Operar `ARS` (`MONEDA`) es 400.
- Una `LIMIT BUY` en `NEW` reserva cash. Una `LIMIT SELL` en `NEW` reserva acciones.
- El portfolio muestra solo posiciones con cantidad mayor a cero. BMA queda en -10 en el seed y no aparece.
- Las 500 acciones del usuario 1 son METR (id 54), no TXAR (id 49).

Valores esperados para el usuario 1 sobre el seed: 627.500 disponibles y 904.784 de valor total. Si un cambio los mueve, hay un error o una decisión nueva que documentar.

## 7. Testing Strategy

### Tipos de tests

| Clase | Qué es | Base |
|---|---|---|
| `domain/service/BalanceCalculatorTest`, `domain/support/MoneyMathTest`, `application/support/LikePatternsTest` | Unitario, sin Spring | Ninguna |
| `api/PortfolioApiTest`, `api/OrderApiTest`, `api/CancelOrderApiTest`, `api/InstrumentSearchApiTest` | MockMvc, rollback por test, extienden `IntegrationTestSupport` | Testcontainers |
| `infrastructure/persistence/SeedDataTest` | Conteos del seed oficial, extiende `IntegrationTestSupport` | Testcontainers |
| `ChallengeEndToEndTest` | Servidor en puerto real + `RestClient`, verifica filas con `JdbcTemplate` | Testcontainers, seed recargado antes de cada test |

- Lógica de cálculo nueva: test unitario en `domain`, sin contexto de Spring.
- Comportamiento de un endpoint: la clase `*ApiTest` de ese endpoint. Extender `IntegrationTestSupport` y no agregar anotaciones de contexto en la subclase (`@MockitoBean`, `@TestPropertySource`): cada combinación distinta levanta otro contexto y otro contenedor.
- Flujos que escriben y se leen después, o que dependen de HTTP real: `ChallengeEndToEndTest`.
- Nunca H2 y nunca Neon en tests.
- El reloj se fija con `FixedClockConfiguration` (`2026-11-09T18:00:00Z`), compartido por los tests de API y el E2E. No usar `LocalDateTime.now()` sin el `Clock` inyectado.

### Nombres

Los tests describen el comportamiento en una frase, en inglés y camelCase: `amountBelowThePriceIsRejected`, `filledOrdersCannotBeCancelled`. Seguir ese estilo, no `should…When…`.

## 8. Agent Guardrails

- **Tests antes de cerrar.** Ejecutar `./mvnw test` con Docker corriendo y confirmar 0 fallos. Si no se pudo ejecutar, decirlo; no reportar un resultado que no se vio.
- **Cambios mínimos.** No renombrar ni mover fuera de lo pedido. El único reformateo permitido es el de `spring-javaformat:apply`.
- **Credenciales.** Nunca en código, properties, Compose, README ni la colección de Postman. Las de Neon van en `.env` (está en `.gitignore`) o en el entorno. `.env.example` lleva solo placeholders. La URL JDBC va con `sslmode=require` y sin usuario ni contraseña adentro.
- **Neon es de escritura real.** No apuntar a Neon para probar cambios. Solo se usa en el handoff, con `DB_TARGET=neon`.
- **Dependencias nuevas.** Verificar que el artefacto exista en Maven Central y que sea compatible con Boot 4.1 antes de agregarlo.
- **Documentación.** Si cambia un endpoint, un comando o una regla de negocio, actualizar el README, `http/cocos.http` y `postman/cocos-challenge.postman_collection.json`.
