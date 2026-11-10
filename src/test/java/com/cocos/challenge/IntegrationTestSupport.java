package com.cocos.challenge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base for MockMvc tests against the seeded Testcontainers database. Every test runs in a
 * transaction that is rolled back, and all subclasses share one application context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@Transactional
public abstract class IntegrationTestSupport {

	protected static final double MONEY = 0.001;

	@Autowired
	protected MockMvc mockMvc;

	protected ResultActions postOrder(int userId, String json) throws Exception {
		return mockMvc.perform(
				post("/api/v1/users/{userId}/orders", userId).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected static String marketBuy(int instrumentId, int size) {
		return """
				{"instrumentId":%d,"side":"BUY","type":"MARKET","size":%d}
				""".formatted(instrumentId, size);
	}

}
