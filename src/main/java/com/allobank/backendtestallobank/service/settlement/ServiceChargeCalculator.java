package com.allobank.backendtestallobank.service.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class ServiceChargeCalculator {

	private static final String GITHUB_USERNAME = "arif14377";
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	public int percentage() {
		String normalizedUsername = GITHUB_USERNAME.toLowerCase(Locale.ROOT);
		int sum = normalizedUsername.chars().sum();
		return sum % 10;
	}

	public BigDecimal calculateAmount(BigDecimal totalExpenses) {
		return totalExpenses
				.multiply(BigDecimal.valueOf(percentage()))
				.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
	}
}
