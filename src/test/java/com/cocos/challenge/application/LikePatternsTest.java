package com.cocos.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LikePatternsTest {

	@Test
	void escapesLikeWildcardsAndWrapsTheTerm() {
		assertThat(LikePatterns.contains("PAMP")).isEqualTo("%PAMP%");
		assertThat(LikePatterns.contains("100%")).isEqualTo("%100\\%%");
		assertThat(LikePatterns.contains("a_b")).isEqualTo("%a\\_b%");
		assertThat(LikePatterns.contains("\\")).isEqualTo("%\\\\%");
	}
}
