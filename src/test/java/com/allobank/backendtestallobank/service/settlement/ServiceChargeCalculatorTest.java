package com.allobank.backendtestallobank.service.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ServiceChargeCalculatorTest {

	private final ServiceChargeCalculator calculator = new ServiceChargeCalculator();

	@Test
	void calculatesPercentageFromGithubUsernameFormula() {
		assertThat(calculator.percentage()).isZero();
	}

	@Test
	void calculatesServiceChargeAmountFromTotalExpenses() {
		assertThat(calculator.calculateAmount(new BigDecimal("450000.00"))).isEqualByComparingTo("0.00");
	}
}
