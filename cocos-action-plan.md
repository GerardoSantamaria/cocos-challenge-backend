# Plan de acción — Cocos Cap Backend Challenge (Java + Spring Boot)

**Audiencia:** Gerardo (engineering lead).  
**Estado:** revisado contra el challenge y con todas las decisiones cerradas (§5). Listo para arrancar en el paso 1 de §3.  
**Alcance de este documento:** plan y decisiones técnicas únicamente. Sin código.  
**Fuentes:** [backend-challenge.md](https://github.com/cocoscap/cocos-challenge/blob/main/backend-challenge.md), [database.sql](https://github.com/cocoscap/cocos-challenge/blob/main/database.sql), Neon Postgres del challenge (verificado en solo lectura), y [frontend-challenge.md](https://github.com/cocoscap/cocos-challenge/blob/main/frontend-challenge.md) solo como pista de contrato.

**Secretos:** las credenciales de Neon viven únicamente en un archivo `.env` local ignorado por git (ver §2.3). Este plan **no** incluye host, user, password ni URL completa, y ningún archivo del repo debe incluirlos.

---

## 1. Resumen del challenge

### Qué hay que construir

Una **API REST** de brokerage simplificado sobre el esquema seed `users`, `instruments`, `orders`, `marketdata`. El enunciado pide Node.js; **este proyecto se implementa en Java + Spring Boot** (decisión del owner, cualquier librería permitida).

### Capacidades obligatorias

| Capacidad | Comportamiento esperado |
|---|---|
| **Portfolio** | Valor total de la cuenta, pesos disponibles para operar, y posiciones: cantidad, valor monetario de la posición, rendimiento total (%). |
| **Buscar activos** | Listado de instrumentos similares por **ticker y/o nombre**. |
| **Enviar orden** | `BUY` / `SELL`, tipos `MARKET` y `LIMIT`, por cantidad o por monto en pesos. Persistir en `orders` con status y valores correctos. |
| **Cancelar orden** (implícito por reglas) | Solo órdenes en `NEW` → `CANCELLED`. |

### Reglas de dominio clave

- Precios en **pesos (ARS)**. No simular el mercado (no hay matching LIMIT → FILLED).
- Cantidad: acciones exactas **o** monto en ARS → `floor(monto / precio)`, sin fracciones.
- Estados: `NEW` (LIMIT aceptada), `FILLED` (MARKET inmediata), `REJECTED` (no cumple requisitos), `CANCELLED` (cancelada por el usuario).
- Cash modelado como instrumento `type = MONEDA` (ticker `ARS`, id 66 en el seed).
- Transferencias: órdenes `CASH_IN` / `CASH_OUT`.
- Tenencia y cash: derivar de `orders` (columna `size`), sin tabla de posiciones materializada.
- MARKET: precio = último `close` de `marketdata`.
- Posiciones / valor de mercado / rendimiento: usar órdenes `FILLED` por activo.
- Retorno diario: `(close - previousClose) / previousClose`.

### No-funcionales / técnicos del enunciado

- Sin autenticación.
- Al menos **un test funcional** del flujo "enviar orden".
- Documentar supuestos y decisiones de diseño.
- Nice-to-have: colección Postman / Insomnia / REST Client.
- Libertad para ajustar el esquema **si se justifica** en la documentación.

### Datos seed oficiales (`database.sql`)

Archivo corto: solo `CREATE TABLE` + `INSERT`. **Sin índices secundarios, sin UNIQUE en ticker, sin CHECK de enums, sin NOT NULL** fuera de las PK `SERIAL`. Tampoco es idempotente (no usa `IF NOT EXISTS`): no se puede correr dos veces sobre la misma base.

| Tabla | Columnas efectivas en Postgres | Tipos | Constraints |
|---|---|---|---|
| `users` | `id`, `email`, `accountnumber` | `SERIAL`/`varchar` | PK |
| `instruments` | `id`, `ticker`, `name`, `type` | `SERIAL`/`varchar(10\|255\|10)` | PK |
| `orders` | `id`, `instrumentid`, `userid`, `size`, `price`, `type`, `side`, `status`, `datetime` | `INT`, `NUMERIC(10,2)`, `TIMESTAMP` | PK + FK → instruments, users |
| `marketdata` | `id`, `instrumentid`, `high`, `low`, `open`, `close`, `previousclose`, `date` | `NUMERIC(10,2)`, **`DATE`** | PK + FK → instruments |

**Quirk de naming:** el SQL declara columnas en camelCase **sin comillas** (`accountNumber`, `instrumentId`, `previousClose`). Postgres las guarda en **minúsculas sin guiones** (`accountnumber`, `instrumentid`, `previousclose`).

**Volúmenes del seed**

| Tabla | Filas | Patrón |
|---|---|---|
| `users` | 4 | ids 1–4; demo = **user 1** (`emiliano@test.com` / `10001`) |
| `instruments` | 66 | 65 `ACCIONES` + 1 `MONEDA` (`ARS` / `PESOS`, **id 66**) |
| `marketdata` | 126 | 2 fechas (`2023-07-13`, `2023-07-14`) × 63 instrumentos; OHLC a veces `NULL` |
| `orders` | 11 | **todas** de `userid = 1`; mezcla CASH_IN/OUT, BUY/SELL, MARKET/LIMIT, NEW/FILLED/REJECTED/CANCELLED |

**Huecos útiles para tests:** PGR (id 3) e IRCP (id 10) son `ACCIONES` **sin** filas en `marketdata` → MARKET y search deben manejar "sin precio".

### Inconsistencia del seed: posición negativa de BMA

El user 1 tiene, sobre BMA (instrumento 31), una compra `FILLED` de 20 acciones y una venta `FILLED` de 30. Calculado como compras − ventas, tendría **−10 acciones**, algo imposible (no hay venta en corto). Es un error de los datos de ejemplo, no de la fórmula.

**Solución (confirmada por Gerardo, 2026-11-09):**

1. **No tocar los datos** (ni local ni Neon): el challenge dice que el cálculo se hace con lo que hay en `orders`.
2. **El portfolio solo muestra posiciones con cantidad > 0.** BMA no aparece.
3. **El cash sí cuenta esa venta** (+45.900), porque es un movimiento `FILLED` de la tabla.
4. **Cualquier nueva venta de BMA se rechaza**, porque las acciones disponibles son −10 (< size).
5. Documentarlo en el README como inconsistencia conocida del seed.

### Diffs vs el markdown del challenge

| Markdown dice | Realidad SQL / Neon |
|---|---|
| `marketdata… datetime` | Columna **`date` (`DATE`)** |
| Nombres camelCase | Identificadores físicos **lowercase** |
| Stack Node.js | Este proyecto: **Java + Spring Boot** |
| (Frontend) status `PENDING` | Seed y backend: **`NEW`** |

### Diffs vs Neon live (lectura 2026-11-06)

Verificación **read-only** contra el Postgres Neon del challenge (`neondb`, PostgreSQL 18.x, SSL required).

| Chequeo | Resultado |
|---|---|
| Tablas | Exactamente las 4 del seed |
| Columnas / tipos / FKs / PKs | Alineados con `database.sql` (lowercase) |
| Conteos | 4 / 66 / 11 / 126 — **idénticos al seed** |
| Índices extra | Ninguno (solo PK btree) |

**Conclusión:** Neon ya está seeded. No se re-aplica `database.sql` ni se ejecuta DDL contra Neon.

### Estrategia DB (decidida — locked)

| Fase | Base de datos |
|---|---|
| **Desarrollo y tests** | **Testcontainers** (Postgres efímero con `database.sql` como init script). Todos los writes ocurren acá. Neon no se usa. |
| **Handoff (todo desarrollado y probado)** | App apunta a **Neon** para reads + writes, perfil `neon`, credenciales por variables de entorno. |

---

## 2. Arquitectura propuesta

### 2.1 Estilo general

**Monolito modular Spring Boot 4.1.x**, capas clásicas + dominio rico en el servicio de órdenes:

```
com.cocos.challenge
├── api            # Controllers, DTOs request/response, manejo de errores
├── application    # Use cases (Portfolio, Search, PlaceOrder, CancelOrder)
├── domain         # Enums, reglas de negocio, cálculo de balances
├── infrastructure # JPA entities, repositories, config
└── support        # Clock, tipos de excepción
```

**Por qué este split:** el challenge es chico; hexagonal estricto agrega ruido. Separar capas deja el núcleo de órdenes y balances testeable sin HTTP. Un solo módulo Maven.

### 2.2 Modelo de dominio

| Concepto | Notas |
|---|---|
| `User` | id, email, accountNumber |
| `Instrument` | id, ticker, name, type (`ACCIONES` \| `MONEDA`) |
| `Order` | instrumentId, userId, side, size, price, type, status, datetime |
| `MarketData` | instrumentId, OHLC, previousClose, date |
| `Side` | `BUY`, `SELL`, `CASH_IN`, `CASH_OUT` |
| `OrderType` | `MARKET`, `LIMIT` |
| `OrderStatus` | `NEW`, `FILLED`, `REJECTED`, `CANCELLED` |
| `Position` (calculado) | instrument + quantity + avgCost + marketValue + rendimientos |
| `Portfolio` (calculado) | totalAccountValue + availableCash + positions[] |

**Regla central:** la fuente de verdad es `orders`. Posiciones y cash se **recalculan** en cada request.

### 2.3 Persistencia

**Elección: Spring Data JPA + PostgreSQL.**

| Opción | Veredicto |
|---|---|
| **Spring Data JPA** | **Elegida.** Mapeo 1:1 del seed, queries derivadas + `@Query` para agregaciones. |
| JdbcTemplate puro | Más boilerplate; no aporta en este tamaño. |
| jOOQ | Overkill. |
| H2 | **Descartado:** diverge de Postgres en quoting/`NUMERIC`. |

**Estrategia de schema**

1. **Sin cambios de esquema en v1.** Con este volumen (11 órdenes) los índices no se pueden justificar con mediciones. En el README se documenta como recomendación para producción: índices en `orders(userid, status, instrumentid)` y `marketdata(instrumentid, date)`, con el script listo pero **no aplicado**.
2. **Sin Flyway ni `sql.init`.** El seed se carga solo en Testcontainers con `withInitScript("database.sql")` (copia del archivo oficial en `src/test/resources`).
3. `spring.jpa.hibernate.ddl-auto=validate` en todos los perfiles: Hibernate valida el mapeo contra la base y nunca crea ni altera tablas. Esto es lo que protege a Neon en el handoff.
4. **No materializar** tabla `positions`.

**Mapeo JPA — trampa de naming (bloqueante si se ignora)**

- Spring Boot por defecto convierte `accountNumber` → `account_number`. La base tiene `accountnumber`.
- **Decisión:** `@Column(name = "accountnumber")` (etc.) explícito en **todas** las columnas camel del enunciado. `ddl-auto=validate` falla al arrancar si alguno quedó mal.
- `open` / `close` / `date` / `datetime` / `type` / `size`: nombre de columna explícito siempre.
- Tipos: `price` / OHLC → `BigDecimal`; `date` → `LocalDate`; `datetime` → `LocalDateTime`; enums → `@Enumerated(EnumType.STRING)`.

**Datasource y secretos**

- **Desarrollo y tests:** Testcontainers con `@ServiceConnection`. No hay URL ni password que configurar.
- **Correr la app en local:** `./mvnw spring-boot:test-run` levanta la app con un Postgres de Testcontainers (clase `TestCocosApplication` en `src/test`). No hace falta Docker Compose.
- **Handoff a Neon:** perfil `neon`, con las variables estándar de Spring:
  - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<host>/<db>?sslmode=require` (formato JDBC, sin user/password en la URL)
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
- Esos valores viven en un `.env` local **listado en `.gitignore` antes del primer commit**. El README documenta los nombres de las variables con placeholders, nunca los valores.
- **SSL obligatorio** contra Neon (`sslmode=require`). Verificar en el handoff si el driver acepta también `channelBinding=require`; si no lo acepta, se quita solo ese parámetro, nunca `sslmode`.
- Logs en `INFO`: no loguear connection strings ni passwords.

### 2.4 Forma de la API

JSON en **camelCase**. Errores con **ProblemDetail (RFC 9457)**, soportado nativamente por Spring.

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/v1/users/{userId}/portfolio` | Cash, total, posiciones |
| `GET` | `/api/v1/instruments/search?q=` | Búsqueda ticker/nombre |
| `POST` | `/api/v1/users/{userId}/orders` | Crear orden (solo `BUY` / `SELL`) |
| `POST` | `/api/v1/users/{userId}/orders/{orderId}/cancel` | Cancelar si `NEW` |

`userId` en el path porque no hay auth; documentar que en producción vendría del token.

**Body de orden:**

```json
{
  "instrumentId": 47,
  "side": "BUY",
  "type": "MARKET",
  "size": 10,
  "amount": null,
  "price": null
}
```

- `side`: solo `BUY` o `SELL`. `CASH_IN` / `CASH_OUT` → **400** (las transferencias existen en el modelo y en el cálculo, pero no se crean por este endpoint).
- `size` **o** `amount`, exactamente uno. `size` entero > 0; `amount` > 0.
- `price` obligatorio y > 0 si `type = LIMIT`; ignorado si `MARKET`.
- Límites de tipo: `price` y `amount` deben entrar en `NUMERIC(10,2)`; `size` debe entrar en un `INT`.

**Respuesta de orden:** `201 Created` con `{ "id", "instrumentId", "side", "type", "size", "price", "status", "datetime" }`. Una orden `REJECTED` **también** devuelve 201: la orden se creó y quedó persistida con ese estado; el cliente lee `status`.

**Portfolio:**

```json
{
  "availableCash": 627500.00,
  "totalAccountValue": 904784.00,
  "positions": [
    {
      "instrumentId": 47,
      "ticker": "PAMP",
      "name": "Pampa Holding S.A.",
      "quantity": 40,
      "lastPrice": 925.85,
      "marketValue": 37034.00,
      "avgCostPrice": 930.00,
      "totalReturnPct": -0.45,
      "dailyReturnPct": 0.44
    }
  ]
}
```

**Search:** `{ "id", "ticker", "name", "lastPrice", "dailyReturnPct" }` (los dos últimos `null` si no hay `marketdata`).

### 2.5 Flujo de negocio — `PlaceOrder`

Todo dentro de una única transacción `@Transactional`:

1. **Validar request** (Bean Validation, §2.4). Errores de formato → **400**, sin persistir.
2. Usuario inexistente → **404**. Instrumento inexistente → **404**. Instrumento `MONEDA` (ARS) → **400**.
3. **Bloquear la fila del usuario:** `SELECT … FROM users WHERE id = ? FOR UPDATE`. Serializa las órdenes concurrentes del mismo usuario (evita que dos BUY simultáneos gasten el mismo cash).
4. **Resolver precio:** MARKET → `close` del `marketdata` más reciente del instrumento; LIMIT → `price` del request.
   - MARKET sin `marketdata` (PGR, IRCP) → **REJECTED** (`price` = null; `size` = el pedido, o null si vino por monto).
5. **Resolver cantidad:** si vino `amount` → `size = floor(amount / price)`. Si da **0** → **REJECTED**.
6. **Calcular balances** del usuario (§2.6).
7. **Validar disponibilidad:**
   - BUY: `size × price > availableCash` → **REJECTED**.
   - SELL: `size > availableShares` del instrumento → **REJECTED**.
8. Si pasa: MARKET → **FILLED**; LIMIT → **NEW**.
9. Persistir con `datetime = now` (vía `Clock` inyectable, para tests deterministas) y devolver 201.

**Cancel:** orden inexistente o de otro usuario → **404**; status distinto de `NEW` → **409**; si no, `CANCELLED` y 200 con la orden. Cancelar una LIMIT libera su reserva automáticamente (la reserva se calcula, no se guarda).

### 2.6 Cálculos de portfolio y balances (decididos)

Sobre órdenes del usuario:

| Concepto | Fórmula |
|---|---|
| **cashBalance** (contable) | Σ size `CASH_IN` FILLED − Σ size `CASH_OUT` FILLED − Σ (size×price) `BUY` FILLED + Σ (size×price) `SELL` FILLED |
| **reservedCash** | Σ (size×price) de `BUY` `LIMIT` en `NEW` |
| **availableCash** (para operar) | cashBalance − reservedCash |
| **quantity** de un activo | Σ size `BUY` FILLED − Σ size `SELL` FILLED (solo `ACCIONES`) |
| **availableShares** (para vender) | quantity − Σ size `SELL` `LIMIT` en `NEW` |
| **avgCostPrice** | Promedio ponderado de compras FILLED: Σ (size×price) BUY / Σ size BUY. Las ventas no lo modifican (método de costo promedio). |
| **marketValue** | quantity × último `close` |
| **totalReturnPct** | (último `close` − avgCostPrice) / avgCostPrice × 100 |
| **dailyReturnPct** | (`close` − `previousClose`) / `previousClose` × 100, del último día |
| **totalAccountValue** | cashBalance + Σ marketValue de posiciones mostradas |

Reglas de presentación:

- Solo se muestran posiciones con **quantity > 0** (resuelve BMA, §1).
- El total usa **cashBalance** (no availableCash): cargar una LIMIT no baja el valor de la cuenta.
- Posición sin `marketdata`: `lastPrice`, `marketValue` y rendimientos en `null`, no suma al total; documentado. No ocurre en el seed para posiciones del user 1.
- Montos con `BigDecimal`; porcentajes redondeados a 2 decimales (`HALF_UP`) solo al serializar.

**Valores esperados para user 1 sobre el seed** (base del test de regresión):

| Dato | Valor |
|---|---|
| cashBalance | 753.000,00 |
| reservedCash | 125.500,00 (50×710 + 60×1500) |
| availableCash | 627.500,00 |
| PAMP (47) | 40 acciones, avgCost 930, close 925,85, marketValue 37.034,00, total −0,45 %, diario +0,44 % |
| METR (54) | 500 acciones, avgCost 250, close 229,50, marketValue 114.750,00, total −8,20 %, diario −1,08 %. El ticker es MetroGAS, no TXAR (TXAR es el id 49). |
| BMA (31) | −10 → no se muestra |
| totalAccountValue | 904.784,00 |

---

## 3. Plan de acción ordenado (cero → done)

| # | Paso | Depende de | Entregable concreto |
|---|---|---|---|
| **1** | Setup proyecto | — | Spring Boot 4.1.x + Java 21 + Maven desde Initializr, `.gitignore` (incluye `.env`), perfiles default / `neon`, README stub |
| **2** | DB de desarrollo | 1 | Testcontainers + `database.sql` como init script; `TestCocosApplication` para `spring-boot:test-run` |
| **3** | Persistencia | 2 | Entities con `@Column` lowercase + repositories; `ddl-auto=validate` arranca OK; smoke counts 4/66/11/126 |
| **4** | Dominio de balances | 3 | Cálculo puro de cashBalance, reservas, quantity, availableShares, avgCost (§2.6) + unit tests |
| **5** | PlaceOrder + Cancel | 4 | Reglas §2.5 + lock de usuario + transacción |
| **6** | Portfolio | 4 | Agregación + último `close` / `previousClose` |
| **7** | Search | 3 | ILIKE ticker/nombre escapado, sin MONEDA, máx. 20 + último precio + retorno diario |
| **8** | API layer | 5–7 | Controllers, DTOs, validación, `@RestControllerAdvice` con ProblemDetail |
| **9** | Tests | 5, 8 | Test funcional PlaceOrder (requerido) + regresión de portfolio user 1 (§2.6) + unit de balances |
| **10** | Docs + nice-to-have | 8–9 | README (supuestos, inconsistencia BMA, cómo correr, variables de Neon), colección `.http` / Postman, Swagger opcional |
| **11** | Handoff a Neon | 10 | `.env` local con credenciales, perfil `neon`, smoke reads + writes contra Neon |

### Casos mínimos del test funcional de PlaceOrder

`@SpringBootTest` + MockMvc + Testcontainers. Cada test con rollback (`@Transactional`) para que el seed quede intacto entre tests.

- MARKET BUY con fondos → `FILLED` al último `close`.
- BUY por monto → `size = floor(monto / precio)`.
- BUY por monto menor al precio (size 0) → `REJECTED`.
- BUY sin fondos suficientes (considerando la reserva de las LIMIT NEW) → `REJECTED`.
- LIMIT BUY → `NEW`, y el availableCash del portfolio baja.
- SELL sin acciones suficientes → `REJECTED` (incluye BMA, y vender más de lo disponible por LIMIT SELL en NEW).
- MARKET sobre PGR (sin precio) → `REJECTED`.
- Orden sobre ARS → 400; side `CASH_IN` → 400; size y amount juntos → 400; LIMIT sin price → 400.
- Cancel de `NEW` → `CANCELLED`; cancel de `FILLED` → 409; cancel de orden de otro usuario → 404.

### Definition of Done

- [ ] Los tres flujos principales + cancel responden correctamente contra el seed.
- [ ] Portfolio de user 1 coincide con la tabla de §2.6.
- [ ] Órdenes persisten con status/size/price coherentes.
- [ ] `./mvnw test` en verde (Testcontainers, nunca Neon).
- [ ] README con supuestos, decisiones, cómo correr y cómo apuntar a Neon.
- [ ] (Opcional) colección Postman / `.http`.
- [ ] Handoff: smoke contra Neon OK.

---

## 4. Librerías

Generar el proyecto en [start.spring.io](https://start.spring.io) con **Spring Boot 4.1.x, Java 21, Maven** y las dependencias Spring Web, Spring Data JPA, Validation, PostgreSQL Driver y Testcontainers. En Boot 4 eso genera estos artefactos (verificado contra Initializr):

| Dependencia | Para qué |
|---|---|
| `spring-boot-starter-webmvc` | REST |
| `spring-boot-starter-data-jpa` | Persistencia |
| `spring-boot-starter-validation` | Bean Validation en DTOs |
| `postgresql` (runtime) | Driver |
| `spring-boot-starter-*-test` (webmvc, data-jpa, validation) | Tests con MockMvc / `@SpringBootTest` |
| `spring-boot-testcontainers` + `testcontainers-postgresql` + `testcontainers-junit-jupiter` | Postgres real en tests y en `spring-boot:test-run` |
| `springdoc-openapi` *(opcional)* | Swagger UI; verificar la versión compatible con Boot 4 antes de agregarla |

**No se usan:** Flyway (no hay cambios de esquema), Lombok (records de Java alcanzan para DTOs), Security/OAuth, Redis/Kafka.

**Java 21 LTS** (Java 25 también es LTS y válido si ya está instalado).

---

## 5. Decisiones cerradas

Todas confirmadas por Gerardo el 2026-11-09.

| # | Tema | Decisión |
|---|---|---|
| 1 | Posición negativa BMA en el seed | **Confirmada.** No tocar datos; mostrar solo posiciones > 0; el cash cuenta la venta; documentar (§1) |
| 2 | Versión | Spring Boot 4.1.x + Java 21 |
| 3 | Reserva de órdenes LIMIT NEW | Reserva cash (BUY) **y** acciones (SELL) |
| 4 | Cash en el valor total | Total = cashBalance + posiciones; disponible = cashBalance − reservas |
| 5a | Monto que da 0 acciones | `REJECTED` |
| 5b | Orden sobre ARS (MONEDA) | 400 |
| 5c | CASH_IN / CASH_OUT por el endpoint | No; solo BUY / SELL (400) |
| 5d | size / price | > 0 y dentro de los límites de tipo de la columna |
| 6 | Concurrencia | `SELECT … FOR UPDATE` sobre la fila del usuario |
| 7 | Auth | No se implementa (el challenge la excluye) |
| 8 | Bases de datos | Testcontainers en desarrollo y tests; Neon solo en el handoff, sin DDL ni re-seed |
| — | Status de LIMIT | `NEW` |
| — | Endpoint de cancel | Sí |
| — | Rendimiento | Total vs costo promedio ponderado + retorno diario por posición |
| — | Formato JSON | camelCase |
| — | Search | Excluye MONEDA |
| — | MARKET sin precio | `REJECTED` |
| — | Usuario | `userId` en el path; demo = user 1 |
| — | Órdenes rechazadas | Se persisten y devuelven 201 con `status = REJECTED` |

### Riesgos técnicos

| Riesgo | Mitigación |
|---|---|
| Dos órdenes concurrentes del mismo usuario | Lock `FOR UPDATE` de la fila `users` dentro de la transacción |
| Divergencia de naming SQL | `@Column` explícito + `ddl-auto=validate` |
| Mal cálculo de cash / posiciones | Test de regresión con los valores de §2.6 |
| Escribir por error en Neon durante el desarrollo | Tests y dev solo con Testcontainers; perfil `neon` solo en el handoff; `ddl-auto=validate` |
| Credenciales de Neon en git | `.env` en `.gitignore` antes del primer commit; README solo con placeholders |
| Scope creep (materializar posiciones, auth, matching) | Mantener el DoD de §3 |

---

## 6. Local-dev checklist

### En la máquina de Gerardo

- [ ] **JDK 21** — `java -version`
- [ ] **Docker Desktop** corriendo (Testcontainers lo necesita)
- [ ] Maven: se usa el wrapper `./mvnw` que genera Initializr
- [ ] IDE: IntelliJ IDEA (o VS Code + Extension Pack for Java)
- [ ] (Opcional) Postman / REST Client

### Arranque típico (desarrollo)

1. `./mvnw spring-boot:test-run` → levanta Postgres en Testcontainers con el seed y la app.
2. Smoke:
   - `GET /api/v1/users/1/portfolio` (debe coincidir con §2.6)
   - `GET /api/v1/instruments/search?q=PAMP`
   - `POST /api/v1/users/1/orders` (MARKET BUY chico)
3. Tests: `./mvnw test`.

### Handoff → Neon

1. Crear `.env` local (ignorado por git) con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` (URL en formato JDBC con `sslmode=require`).
2. Exportar las variables y correr con el perfil `neon`.
3. `ddl-auto=validate`: si el mapeo no coincide, la app no arranca (no altera nada).
4. Smoke reads + writes contra Neon. Tener presente que los writes del smoke quedan en la base del recruiter.
