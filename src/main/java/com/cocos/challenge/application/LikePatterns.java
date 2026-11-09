package com.cocos.challenge.application;

public final class LikePatterns {

	private LikePatterns() {
	}

	public static String contains(String raw) {
		String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}
}
