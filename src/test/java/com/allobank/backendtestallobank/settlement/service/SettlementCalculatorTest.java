package com.allobank.backendtestallobank.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SettlementCalculatorTest {

	private static final UUID ARIF = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID BUDI = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CITRA = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID DEWI = UUID.fromString("00000000-0000-0000-0000-000000000004");

	private final SettlementCalculator calculator = new SettlementCalculator();

	@Test
	void onePayerMultipleDebtorsProducesTransfersToPayer() {
		SettlementResult result = calculator.calculate(
				participants(ARIF, BUDI, CITRA),
				List.of(bill(ARIF, "300000.00")),
				List.of(debt(ARIF, "100000.00"), debt(BUDI, "100000.00"), debt(CITRA, "100000.00")));

		assertThat(result.totalExpenses()).isEqualByComparingTo("300000.00");
		assertThat(result.balances().get(ARIF)).isEqualByComparingTo("200000.00");
		assertThat(result.balances().get(BUDI)).isEqualByComparingTo("-100000.00");
		assertThat(result.balances().get(CITRA)).isEqualByComparingTo("-100000.00");
		assertThat(result.transfers())
				.containsExactly(
						new SettlementTransfer(BUDI, ARIF, new BigDecimal("100000.00")),
						new SettlementTransfer(CITRA, ARIF, new BigDecimal("100000.00")));
	}

	@Test
	void multiplePayersAndPayerAlsoDebtorAreOffsetBeforeTransfer() {
		SettlementResult result = calculator.calculate(
				participants(ARIF, BUDI, CITRA),
				List.of(bill(ARIF, "300000.00"), bill(BUDI, "150000.00")),
				List.of(
						debt(ARIF, "100000.00"), debt(BUDI, "100000.00"), debt(CITRA, "100000.00"),
						debt(ARIF, "50000.00"), debt(BUDI, "50000.00"), debt(CITRA, "50000.00")));

		assertThat(result.balances().get(ARIF)).isEqualByComparingTo("150000.00");
		assertThat(result.balances().get(BUDI)).isEqualByComparingTo("0.00");
		assertThat(result.balances().get(CITRA)).isEqualByComparingTo("-150000.00");
		assertThat(result.transfers())
				.containsExactly(new SettlementTransfer(CITRA, ARIF, new BigDecimal("150000.00")));
	}

	@Test
	void multipleBillsWithPositiveNegativeAndZeroBalancesAreCalculated() {
		SettlementResult result = calculator.calculate(
				participants(ARIF, BUDI, CITRA),
				List.of(bill(ARIF, "120000.00"), bill(BUDI, "60000.00"), bill(CITRA, "60000.00")),
				List.of(
						debt(ARIF, "40000.00"), debt(BUDI, "40000.00"), debt(CITRA, "40000.00"),
						debt(ARIF, "20000.00"), debt(BUDI, "20000.00"), debt(CITRA, "20000.00"),
						debt(ARIF, "20000.00"), debt(BUDI, "20000.00"), debt(CITRA, "20000.00")));

		assertThat(result.balances().get(ARIF)).isEqualByComparingTo("40000.00");
		assertThat(result.balances().get(BUDI)).isEqualByComparingTo("-20000.00");
		assertThat(result.balances().get(CITRA)).isEqualByComparingTo("-20000.00");
		assertThat(result.transfers()).hasSize(2);
	}

	@Test
	void offsettingBalancesProduceNoTransfers() {
		SettlementResult result = calculator.calculate(
				participants(ARIF, BUDI),
				List.of(bill(ARIF, "100000.00"), bill(BUDI, "100000.00")),
				List.of(
						debt(ARIF, "50000.00"), debt(BUDI, "50000.00"),
						debt(ARIF, "50000.00"), debt(BUDI, "50000.00")));

		assertThat(result.balances().get(ARIF)).isEqualByComparingTo("0.00");
		assertThat(result.balances().get(BUDI)).isEqualByComparingTo("0.00");
		assertThat(result.transfers()).isEmpty();
	}

	@Test
	void minimizedTransfersUseAtMostDebtorsPlusCreditorsMinusOneWhenPossible() {
		SettlementResult result = calculator.calculate(
				participants(ARIF, BUDI, CITRA, DEWI),
				List.of(bill(ARIF, "250000.00"), bill(BUDI, "150000.00")),
				List.of(
						debt(CITRA, "250000.00"),
						debt(DEWI, "150000.00")));

		assertThat(result.transfers())
				.containsExactly(
						new SettlementTransfer(CITRA, ARIF, new BigDecimal("250000.00")),
						new SettlementTransfer(DEWI, BUDI, new BigDecimal("150000.00")));
	}

	@Test
	void bigDecimalPrecisionIsPreservedForCents() {
		SettlementResult result = calculator.calculate(
				participants(ARIF, BUDI, CITRA),
				List.of(bill(ARIF, "0.30")),
				List.of(debt(ARIF, "0.10"), debt(BUDI, "0.10"), debt(CITRA, "0.10")));

		assertThat(result.totalExpenses()).isEqualByComparingTo("0.30");
		assertThat(result.balances().get(ARIF)).isEqualByComparingTo("0.20");
		assertThat(result.transfers())
				.containsExactly(
						new SettlementTransfer(BUDI, ARIF, new BigDecimal("0.10")),
						new SettlementTransfer(CITRA, ARIF, new BigDecimal("0.10")));
	}

	private List<SettlementParticipant> participants(UUID... userIds) {
		return List.of(userIds).stream()
				.map(userId -> new SettlementParticipant(userId, userId.toString()))
				.toList();
	}

	private SettlementBill bill(UUID payerId, String amount) {
		return new SettlementBill(payerId, new BigDecimal(amount));
	}

	private SettlementDebt debt(UUID debtorId, String amount) {
		return new SettlementDebt(debtorId, new BigDecimal(amount));
	}
}
