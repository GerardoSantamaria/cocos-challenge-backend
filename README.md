# Cocos Challenge Backend

API de brokerage sobre el esquema del [backend challenge](https://github.com/cocoscap/cocos-challenge/blob/main/backend-challenge.md). El enunciado pide Node.js; esta solución está en **Java 21 + Spring Boot 4.1**.

No hay autenticación: el `userId` va en el path. En un sistema real saldría del token.

## Qué hace

| Método | Path | Qué devuelve |
|---|---|---|
| `GET` | `/api/v1/users/{userId}/portfolio` | Pesos disponibles, valor total de la cuenta y posiciones |
| `GET` | `/api/v1/instruments/search?q=` | Acciones por ticker o nombre (máximo 20). No incluye `ARS` |
| `POST` | `/api/v1/users/{userId}/orders` | Crea una orden `BUY` o `SELL`, `MARKET` o `LIMIT` |
| `POST` | `/api/v1/users/{userId}/orders/{orderId}/cancel` | Cancela una orden `NEW` |

Las respuestas van en camelCase. Una orden rechazada se guarda y responde **201** con `status: REJECTED`. El cliente lee el estado.

## Cómo correrlo

Hay dos formas. Las dos usan el `database.sql` oficial cuando la base es local. Neon no se toca salvo que lo pidas con `DB_TARGET=neon`.

### En la máquina, con Testcontainers

Hace falta JDK 21 y Docker. La base la levanta Testcontainers: no hay que pasar credenciales.

```bash
./mvnw test
./mvnw spring-boot:test-run
```

`test` corre la suite contra Postgres efímero. `spring-boot:test-run` deja la API en `http://localhost:8080` contra esa misma base. Los ejemplos sueltos están en `http/cocos.http`.

El código sigue [Spring Java Format](https://github.com/spring-io/spring-javaformat). El build falla si un archivo no respeta el formato; `./mvnw spring-javaformat:apply` lo corrige.

### Con Docker Compose

Hace falta Docker. La variable `DB_TARGET` elige la base. Compose la lee del shell o de un `.env` en la raíz del repo (está en `.gitignore`; el modelo es `.env.example`).

| `DB_TARGET` | Base | Qué pasa con el esquema |
|---|---|---|
| `local` (default) | Postgres 17 del Compose, con el seed oficial | Se crea al iniciar el contenedor de la base |
| `neon` | La base de Neon, por variables de entorno | No se migra ni se vuelve a cargar el seed |

`local` dentro de Compose no es Testcontainers. Testcontainers corre en la JVM de `./mvnw` y necesitaría el socket de Docker dentro de la imagen. El Compose local es un Postgres aparte, con el mismo `database.sql`.

Base local:

```bash
docker compose --profile local up --build
```

Neon. Las tres variables tienen que estar definidas, la URL en formato JDBC con `sslmode=require`, y el usuario y la contraseña fuera de la URL:

```bash
cp .env.example .env
# en .env: DB_TARGET=neon y SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
docker compose up --build
```

Con `DB_TARGET=neon` el servicio `postgres` no arranca (`--profile local` es lo que lo incluye). El contenedor de la app rechaza el arranque si falta alguna variable, si la URL trae usuario o contraseña, o si no exige SSL.

La API queda en `http://localhost:8080`. Con `DB_TARGET=local`, Postgres también queda en `localhost:5433`.

## Colección de Postman

`postman/cocos-challenge.postman_collection.json` cubre los cuatro endpoints: portfolio, búsqueda, alta de órdenes y cancelación. Incluye el usuario del seed, compras y ventas `MARKET` y `LIMIT` (por cantidad y por monto), rechazos que se guardan, pedidos que responden 400 o 404, y la cancelación de una orden `NEW`, de una `FILLED` y de una orden ajena.

En Postman: Import → ese archivo. La variable `baseUrl` sale en `http://localhost:8080`. Las peticiones que escriben modifican la base. Cancelar la orden `5` del seed solo funciona una vez, mientras siga en `NEW`.

Ejemplos:

```bash
curl http://localhost:8080/api/v1/users/1/portfolio
curl "http://localhost:8080/api/v1/instruments/search?q=PAMP"
curl -X POST http://localhost:8080/api/v1/users/1/orders \
  -H 'Content-Type: application/json' \
  -d '{"instrumentId":47,"side":"BUY","type":"MARKET","size":1}'
```

## Portfolio del usuario 1

Calculado solo con órdenes `FILLED` (y reservas de las `LIMIT` en `NEW`):

| Dato | Valor |
|---|---|
| Pesos disponibles | 627.500,00 |
| Valor total | 904.784,00 |
| PAMP (id 47) | 40 acciones, costo promedio 930, último precio 925,85 |
| METR (id 54) | 500 acciones, costo promedio 250, último precio 229,50 |

El valor total usa el cash contable más el valor de mercado de las posiciones. Los pesos disponibles restan el cash reservado por compras límite abiertas. El rendimiento total se compara contra el costo promedio ponderado de las compras. El rendimiento diario compara `close` con `previousClose`.

## Decisiones

- **Posición negativa de BMA.** El seed vende 30 acciones de BMA (id 31) y solo compra 20. No se tocan los datos. El portfolio muestra solo cantidades mayores a cero, así que BMA no aparece. El cash sí cuenta esa venta. Una venta nueva de BMA queda `REJECTED`.
- **METR, no TXAR.** La orden de 500 acciones es el instrumento 54, MetroGAS (`METR`). TXAR es el id 49.
- **Órdenes `MARKET`** se ejecutan al último `close` y quedan `FILLED`. Si no hay precio (PGR, IRCP), quedan `REJECTED`.
- **Órdenes `LIMIT`** quedan `NEW`. No hay matching: el challenge pide no simular el mercado.
- **Cantidad por monto:** `floor(monto / precio)`. Si da cero acciones, la orden se guarda `REJECTED`.
- **Reservas.** Una `LIMIT` `BUY` en `NEW` reserva cash. Una `LIMIT` `SELL` en `NEW` reserva acciones. Cancelarla libera la reserva.
- **`POST /orders` solo acepta `BUY` y `SELL`.** `CASH_IN` y `CASH_OUT` existen en el modelo y entran en el cálculo del cash, pero este endpoint no las crea. Tampoco se opera el instrumento `ARS`.
- **`size` y `price` tienen que ser mayores a cero** y entrar en las columnas (`INT` y `NUMERIC(10,2)`).
- **Concurrencia.** Al crear o cancelar una orden se bloquea la fila del usuario (`SELECT … FOR UPDATE`).
- **Esquema.** No se modifica. `ddl-auto=validate` solo comprueba el mapeo. Los nombres físicos son los de Postgres, en minúsculas (`accountnumber`, `instrumentid`, `previousclose`, `date`). Índices sugeridos, sin aplicar, en `docs/optional-indexes.sql`.
- **Columnas que el enunciado nombra distinto.** `marketdata` tiene `date`, no `datetime`.

## Neon

Los tests y `spring-boot:test-run` no se conectan a Neon. El handoff usa las mismas variables que el Compose, y las credenciales quedan fuera del repo.

Por Compose: `DB_TARGET=neon` en `.env`, como está arriba.

Sin Compose, en la máquina:

```bash
set -a && source .env && set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=neon
```

La URL va en formato JDBC, con `sslmode=require`, y sin usuario ni contraseña adentro. `ddl-auto=validate` solo comprueba el mapeo: la aplicación no crea tablas ni vuelve a cargar el seed.
