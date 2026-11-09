package com.cocos.challenge;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.cocos.challenge.infrastructure.persistence.InstrumentRepository;
import com.cocos.challenge.infrastructure.persistence.MarketDataRepository;
import com.cocos.challenge.infrastructure.persistence.OrderRepository;
import com.cocos.challenge.infrastructure.persistence.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ChallengeApiTest.FixedClockConfiguration.class})
@Transactional
class ChallengeApiTest {

	private static final double MONEY = 0.001;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private InstrumentRepository instrumentRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private MarketDataRepository marketDataRepository;

	@Test
	void seedCountsMatchTheOfficialDump() {
		org.assertj.core.api.Assertions.assertThat(userRepository.count()).isEqualTo(4);
		org.assertj.core.api.Assertions.assertThat(instrumentRepository.count()).isEqualTo(66);
		org.assertj.core.api.Assertions.assertThat(orderRepository.count()).isEqualTo(11);
		org.assertj.core.api.Assertions.assertThat(marketDataRepository.count()).isEqualTo(126);
	}

	@Test
	void portfolioOfTheSeedUserMatchesTheAgreedFigures() throws Exception {
		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableCash", closeTo(627500.00, MONEY)))
				.andExpect(jsonPath("$.totalAccountValue", closeTo(904784.00, MONEY)))
				.andExpect(jsonPath("$.positions", hasSize(2)))
				.andExpect(jsonPath("$.positions[0].instrumentId").value(54))
				.andExpect(jsonPath("$.positions[0].ticker").value("METR"))
				.andExpect(jsonPath("$.positions[0].name").value("MetroGAS S.A."))
				.andExpect(jsonPath("$.positions[0].quantity").value(500))
				.andExpect(jsonPath("$.positions[0].lastPrice", closeTo(229.50, MONEY)))
				.andExpect(jsonPath("$.positions[0].marketValue", closeTo(114750.00, MONEY)))
				.andExpect(jsonPath("$.positions[0].avgCostPrice", closeTo(250.00, MONEY)))
				.andExpect(jsonPath("$.positions[0].totalReturnPct", closeTo(-8.20, MONEY)))
				.andExpect(jsonPath("$.positions[0].dailyReturnPct", closeTo(-1.08, MONEY)))
				.andExpect(jsonPath("$.positions[1].instrumentId").value(47))
				.andExpect(jsonPath("$.positions[1].ticker").value("PAMP"))
				.andExpect(jsonPath("$.positions[1].quantity").value(40))
				.andExpect(jsonPath("$.positions[1].lastPrice", closeTo(925.85, MONEY)))
				.andExpect(jsonPath("$.positions[1].marketValue", closeTo(37034.00, MONEY)))
				.andExpect(jsonPath("$.positions[1].avgCostPrice", closeTo(930.00, MONEY)))
				.andExpect(jsonPath("$.positions[1].totalReturnPct", closeTo(-0.45, MONEY)))
				.andExpect(jsonPath("$.positions[1].dailyReturnPct", closeTo(0.44, MONEY)));
	}

	@Test
	void portfolioOfAUserWithoutOrdersIsEmpty() throws Exception {
		mockMvc.perform(get("/api/v1/users/2/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableCash", closeTo(0, MONEY)))
				.andExpect(jsonPath("$.totalAccountValue", closeTo(0, MONEY)))
				.andExpect(jsonPath("$.positions", hasSize(0)));
	}

	@Test
	void unknownUserPortfolioIsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/users/99/portfolio")).andExpect(status().isNotFound());
	}

	@Test
	void orderForAnUnknownUserIsNotFound() throws Exception {
		postOrder(99, marketBuy(47, 1)).andExpect(status().isNotFound());
	}

	@Test
	void marketBuyFillsAtTheLatestClose() throws Exception {
		postOrder(1, marketBuy(47, 1))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.side").value("BUY"))
				.andExpect(jsonPath("$.type").value("MARKET"))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.price", closeTo(925.85, MONEY)))
				.andExpect(jsonPath("$.instrumentId").value(47))
				.andExpect(jsonPath("$.datetime").value("2026-11-09T18:00:00"));
	}

	@Test
	void amountBuysTheLargestWholeNumberOfShares() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","amount":2000}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.price", closeTo(925.85, MONEY)));
	}

	@Test
	void amountBelowThePriceIsRejected() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","amount":100}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(0))
				.andExpect(jsonPath("$.price", closeTo(925.85, MONEY)));
	}

	@Test
	void buyThatNeedsReservedCashIsRejected() throws Exception {
		postOrder(1, marketBuy(47, 700))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(700));
	}

	@Test
	void limitBuyReservesCashWithoutChangingTheAccountValue() throws Exception {
		postOrder(1, """
				{"instrumentId":1,"side":"BUY","type":"LIMIT","size":1,"price":100}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.price", closeTo(100, MONEY)));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableCash", closeTo(627400.00, MONEY)))
				.andExpect(jsonPath("$.totalAccountValue", closeTo(904784.00, MONEY)));
	}

	@Test
	void sellWithoutSharesIsRejected() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"SELL","type":"MARKET","size":41}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void sellOfTheShortSeedPositionIsRejected() throws Exception {
		postOrder(1, """
				{"instrumentId":31,"side":"SELL","type":"MARKET","size":1}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void openLimitSellReservesShares() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"SELL","type":"LIMIT","size":40,"price":1000}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NEW"));

		postOrder(1, """
				{"instrumentId":47,"side":"SELL","type":"MARKET","size":1}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void marketOrderWithoutAPriceIsRejected() throws Exception {
		postOrder(1, marketBuy(3, 5))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.price").value(nullValue()));

		postOrder(1, """
				{"instrumentId":3,"side":"BUY","type":"MARKET","amount":1000}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(nullValue()))
				.andExpect(jsonPath("$.price").value(nullValue()));
	}

	@Test
	void ordersOnCurrencyAreBadRequests() throws Exception {
		postOrder(1, marketBuy(66, 1)).andExpect(status().isBadRequest());
	}

	@Test
	void cashTransfersAreBadRequests() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"CASH_IN","type":"MARKET","size":1}
				""").andExpect(status().isBadRequest());
	}

	@Test
	void malformedOrdersAreBadRequests() throws Exception {
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","size":1,"amount":10}
				""").andExpect(status().isBadRequest());
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"LIMIT","size":1}
				""").andExpect(status().isBadRequest());
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","size":0}
				""").andExpect(status().isBadRequest());
		postOrder(1, """
				{"instrumentId":47,"side":"BUY","type":"MARKET","size":1,"extra":true}
				""").andExpect(status().isBadRequest());
	}

	@Test
	void unknownInstrumentIsNotFound() throws Exception {
		postOrder(1, marketBuy(99, 1)).andExpect(status().isNotFound());
	}

	@Test
	void cancelReleasesTheReservedCashOfANewOrder() throws Exception {
		mockMvc.perform(post("/api/v1/users/1/orders/5/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(get("/api/v1/users/1/portfolio"))
				.andExpect(jsonPath("$.availableCash", closeTo(663000.00, MONEY)))
				.andExpect(jsonPath("$.totalAccountValue", closeTo(904784.00, MONEY)));
	}

	@Test
	void filledOrdersCannotBeCancelled() throws Exception {
		mockMvc.perform(post("/api/v1/users/1/orders/2/cancel")).andExpect(status().isConflict());
	}

	@Test
	void aUserCannotCancelSomeoneElsesOrder() throws Exception {
		mockMvc.perform(post("/api/v1/users/2/orders/5/cancel")).andExpect(status().isNotFound());
	}

	@Test
	void searchMatchesTickerOrNameAndExcludesCurrency() throws Exception {
		mockMvc.perform(get("/api/v1/instruments/search").param("q", "pamp"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].ticker").value("PAMP"))
				.andExpect(jsonPath("$[0].lastPrice", closeTo(925.85, MONEY)))
				.andExpect(jsonPath("$[0].dailyReturnPct", closeTo(0.44, MONEY)));

		mockMvc.perform(get("/api/v1/instruments/search").param("q", "PESOS"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/api/v1/instruments/search").param("q", "%"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/api/v1/instruments/search").param("q", " "))
				.andExpect(status().isBadRequest());
	}

	private ResultActions postOrder(int userId, String json) throws Exception {
		return mockMvc.perform(post("/api/v1/users/{userId}/orders", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private static String marketBuy(int instrumentId, int size) {
		return """
				{"instrumentId":%d,"side":"BUY","type":"MARKET","size":%d}
				""".formatted(instrumentId, size);
	}

	@TestConfiguration
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-11-09T18:00:00Z"), ZoneOffset.UTC);
		}
	}
}
