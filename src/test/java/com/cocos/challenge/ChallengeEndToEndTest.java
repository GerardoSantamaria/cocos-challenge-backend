package com.cocos.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
class ChallengeEndToEndTest {

	private static final double MONEY = 0.001;

	private static final String SEED_INSERTS = seedInserts();

	@LocalServerPort
	private int port;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	private RestClient client;

	@BeforeEach
	void resetDatabaseAndClient() throws Exception {
		try (var connection = dataSource.getConnection()) {
			try (var statement = connection.createStatement()) {
				statement.execute("TRUNCATE TABLE orders, marketdata, instruments, users RESTART IDENTITY CASCADE");
			}
			ScriptUtils.executeSqlScript(connection,
					new ByteArrayResource(SEED_INSERTS.getBytes(StandardCharsets.UTF_8)));
		}
		client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
	}

	@Test
	void portfolioAndSearchAreServedOverHttp() {
		HttpResult portfolio = get("/api/v1/users/1/portfolio");

		assertThat(portfolio.status().value()).isEqualTo(200);
		assertThat(number(portfolio, "$.availableCash")).isCloseTo(627500.00, within(MONEY));
		assertThat(number(portfolio, "$.totalAccountValue")).isCloseTo(904784.00, within(MONEY));
		assertThat(text(portfolio, "$.positions[0].ticker")).isEqualTo("METR");
		assertThat(integer(portfolio, "$.positions[0].quantity")).isEqualTo(500);
		assertThat(text(portfolio, "$.positions[1].ticker")).isEqualTo("PAMP");
		assertThat(integer(portfolio, "$.positions[1].quantity")).isEqualTo(40);

		HttpResult search = get("/api/v1/instruments/search?q=pamp");
		assertThat(search.status().value()).isEqualTo(200);
		assertThat(text(search, "$[0].ticker")).isEqualTo("PAMP");
		assertThat(number(search, "$[0].lastPrice")).isCloseTo(925.85, within(MONEY));

		assertThat(get("/api/v1/instruments/search?q=PESOS").body()).isEqualTo("[]");
		assertThat(get("/api/v1/users/99/portfolio").status().value()).isEqualTo(404);
	}

	@Test
	void marketBuyIsFilledAndThePortfolioAndTheRowChange() {
		HttpResult created = post("/api/v1/users/1/orders", marketBuy(47, 1));

		assertThat(created.status().value()).isEqualTo(201);
		assertThat(text(created, "$.status")).isEqualTo("FILLED");
		assertThat(text(created, "$.side")).isEqualTo("BUY");
		assertThat(text(created, "$.type")).isEqualTo("MARKET");
		assertThat(integer(created, "$.size")).isEqualTo(1);
		assertThat(number(created, "$.price")).isCloseTo(925.85, within(MONEY));
		assertThat(text(created, "$.datetime")).isEqualTo("2026-11-09T18:00:00");
		int orderId = integer(created, "$.id");

		Map<String, Object> row = jdbc
			.queryForMap("select status, size, price, userid, instrumentid from orders where id = ?", orderId);
		assertThat(row.get("status")).isEqualTo("FILLED");
		assertThat(row.get("size")).isEqualTo(1);
		assertThat(row.get("userid")).isEqualTo(1);
		assertThat(row.get("instrumentid")).isEqualTo(47);
		assertThat((BigDecimal) row.get("price")).isEqualByComparingTo("925.85");

		HttpResult portfolio = get("/api/v1/users/1/portfolio");
		assertThat(number(portfolio, "$.availableCash")).isCloseTo(626574.15, within(MONEY));
		assertThat(number(portfolio, "$.totalAccountValue")).isCloseTo(904784.00, within(MONEY));
		assertThat(pamp(portfolio, "quantity")).isEqualTo(41.0);
		assertThat(pamp(portfolio, "marketValue")).isCloseTo(37959.85, within(MONEY));
	}

	@Test
	void limitBuyReservesCashUntilTheOrderIsCancelled() {
		HttpResult created = post("/api/v1/users/1/orders", """
				{"instrumentId":1,"side":"BUY","type":"LIMIT","size":1,"price":100}
				""");
		assertThat(created.status().value()).isEqualTo(201);
		assertThat(text(created, "$.status")).isEqualTo("NEW");
		int orderId = integer(created, "$.id");

		HttpResult reserved = get("/api/v1/users/1/portfolio");
		assertThat(number(reserved, "$.availableCash")).isCloseTo(627400.00, within(MONEY));
		assertThat(number(reserved, "$.totalAccountValue")).isCloseTo(904784.00, within(MONEY));
		assertThat(jdbc.queryForObject("select status from orders where id = ?", String.class, orderId))
			.isEqualTo("NEW");

		HttpResult cancelled = post("/api/v1/users/1/orders/" + orderId + "/cancel", "");
		assertThat(cancelled.status().value()).isEqualTo(200);
		assertThat(text(cancelled, "$.status")).isEqualTo("CANCELLED");

		HttpResult released = get("/api/v1/users/1/portfolio");
		assertThat(number(released, "$.availableCash")).isCloseTo(627500.00, within(MONEY));
		assertThat(jdbc.queryForObject("select status from orders where id = ?", String.class, orderId))
			.isEqualTo("CANCELLED");
	}

	@Test
	void rejectedOrderIsStoredAndThePortfolioStaysTheSame() {
		HttpResult created = post("/api/v1/users/1/orders", marketBuy(47, 700));

		assertThat(created.status().value()).isEqualTo(201);
		assertThat(text(created, "$.status")).isEqualTo("REJECTED");
		int orderId = integer(created, "$.id");
		assertThat(jdbc.queryForObject("select status from orders where id = ?", String.class, orderId))
			.isEqualTo("REJECTED");
		assertThat(orderCount()).isEqualTo(12);

		HttpResult portfolio = get("/api/v1/users/1/portfolio");
		assertThat(number(portfolio, "$.availableCash")).isCloseTo(627500.00, within(MONEY));
		assertThat(number(portfolio, "$.totalAccountValue")).isCloseTo(904784.00, within(MONEY));
		assertThat(pamp(portfolio, "quantity")).isEqualTo(40.0);
	}

	@Test
	void filledOrdersCannotBeCancelledAndAnotherUsersOrderIsNotFound() {
		HttpResult conflict = post("/api/v1/users/1/orders/2/cancel", "");
		assertThat(conflict.status().value()).isEqualTo(409);
		assertThat(jdbc.queryForObject("select status from orders where id = 2", String.class)).isEqualTo("FILLED");

		HttpResult missing = post("/api/v1/users/2/orders/5/cancel", "");
		assertThat(missing.status().value()).isEqualTo(404);
		assertThat(jdbc.queryForObject("select status from orders where id = 5", String.class)).isEqualTo("NEW");
	}

	@Test
	void invalidOrdersDoNotInsertRows() {
		assertThat(post("/api/v1/users/1/orders", marketBuy(66, 1)).status().value()).isEqualTo(400);
		assertThat(post("/api/v1/users/1/orders", """
				{"instrumentId":47,"side":"CASH_IN","type":"MARKET","size":1}
				""").status().value()).isEqualTo(400);
		assertThat(post("/api/v1/users/1/orders", """
				{"instrumentId":47,"side":"BUY","type":"MARKET","size":1,"amount":10}
				""").status().value()).isEqualTo(400);
		assertThat(post("/api/v1/users/99/orders", marketBuy(47, 1)).status().value()).isEqualTo(404);
		assertThat(orderCount()).isEqualTo(11);
	}

	@Test
	void concurrentBuysCannotSpendTheSameCash() throws Exception {
		int buyers = 10;
		CountDownLatch start = new CountDownLatch(1);
		List<Future<HttpResult>> responses = new ArrayList<>();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < buyers; i++) {
				responses.add(executor.submit(() -> {
					start.await();
					return post("/api/v1/users/1/orders", marketBuy(47, 400));
				}));
			}
			start.countDown();
			List<String> statuses = new ArrayList<>();
			for (Future<HttpResult> response : responses) {
				HttpResult result = response.get(30, TimeUnit.SECONDS);
				assertThat(result.status().value()).isEqualTo(201);
				statuses.add(text(result, "$.status"));
			}
			assertThat(statuses).filteredOn("FILLED"::equals).hasSize(1);
			assertThat(statuses).filteredOn("REJECTED"::equals).hasSize(buyers - 1);
		}

		assertThat(jdbc.queryForObject("select count(*) from orders where userid = 1 and instrumentid = 47"
				+ " and size = 400 and status = 'FILLED'", Long.class))
			.isEqualTo(1);
		assertThat(orderCount()).isEqualTo(21);

		HttpResult portfolio = get("/api/v1/users/1/portfolio");
		assertThat(number(portfolio, "$.availableCash")).isCloseTo(257160.00, within(MONEY));
		assertThat(number(portfolio, "$.totalAccountValue")).isCloseTo(904784.00, within(MONEY));
		assertThat(pamp(portfolio, "quantity")).isEqualTo(440.0);
	}

	private HttpResult get(String path) {
		return exchange(client.get().uri(path));
	}

	private HttpResult post(String path, String json) {
		RestClient.RequestBodySpec request = client.post().uri(path);
		if (!json.isEmpty()) {
			request.contentType(MediaType.APPLICATION_JSON).body(json);
		}
		return exchange(request);
	}

	private HttpResult exchange(RestClient.RequestHeadersSpec<?> request) {
		return request.exchange((httpRequest, response) -> {
			String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
			return new HttpResult(response.getStatusCode(), raw);
		});
	}

	private static String text(HttpResult result, String path) {
		return JsonPath.parse(result.body()).read(path, String.class);
	}

	private static double number(HttpResult result, String path) {
		return ((Number) JsonPath.parse(result.body()).read(path)).doubleValue();
	}

	private static int integer(HttpResult result, String path) {
		return ((Number) JsonPath.parse(result.body()).read(path)).intValue();
	}

	private static double pamp(HttpResult portfolio, String field) {
		List<Number> values = JsonPath.parse(portfolio.body()).read("$.positions[?(@.ticker == 'PAMP')]." + field);
		assertThat(values).hasSize(1);
		return values.getFirst().doubleValue();
	}

	private long orderCount() {
		Long count = jdbc.queryForObject("select count(*) from orders", Long.class);
		return count == null ? 0 : count;
	}

	private static String marketBuy(int instrumentId, int size) {
		return """
				{"instrumentId":%d,"side":"BUY","type":"MARKET","size":%d}
				""".formatted(instrumentId, size);
	}

	private static String seedInserts() {
		try {
			String sql = new ClassPathResource("database.sql").getContentAsString(StandardCharsets.UTF_8);
			int from = sql.indexOf("INSERT INTO");
			if (from < 0) {
				throw new IllegalStateException("database.sql has no INSERT statements");
			}
			return sql.substring(from);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not read database.sql", ex);
		}
	}

	private record HttpResult(HttpStatusCode status, String body) {
	}

}
