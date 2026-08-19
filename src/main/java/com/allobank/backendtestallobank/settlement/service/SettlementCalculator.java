package com.allobank.backendtestallobank.settlement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SettlementCalculator {

	private static final int MONEY_SCALE = 2;

	public SettlementResult calculate(
			List<SettlementParticipant> participants,
			List<SettlementBill> bills,
			List<SettlementDebt> debts) {
		Map<UUID, BigDecimal> balances = new LinkedHashMap<>();
		for (SettlementParticipant participant : participants) {
			balances.put(participant.id(), zero());
		}

		BigDecimal totalExpenses = zero();
		for (SettlementBill bill : bills) {
			BigDecimal amount = money(bill.amount());
			totalExpenses = totalExpenses.add(amount);
			balances.computeIfPresent(bill.payerId(), (userId, current) -> current.add(amount));
		}

		for (SettlementDebt debt : debts) {
			BigDecimal amount = money(debt.amount());
			balances.computeIfPresent(debt.debtorId(), (userId, current) -> current.subtract(amount));
		}

		return new SettlementResult(totalExpenses, balances, transfersFrom(balances));
	}

	private List<SettlementTransfer> transfersFrom(Map<UUID, BigDecimal> balances) {
		List<Balance> debtors = new ArrayList<>();
		List<Balance> creditors = new ArrayList<>();

		balances.forEach((userId, balance) -> {
			if (balance.signum() < 0) {
				debtors.add(new Balance(userId, balance.abs()));
			}
			else if (balance.signum() > 0) {
				creditors.add(new Balance(userId, balance));
			}
		});

		Comparator<Balance> largestFirst = Comparator
				.comparing(Balance::amount)
				.reversed()
				.thenComparing(Balance::userId);
		debtors.sort(largestFirst);
		creditors.sort(largestFirst);

		List<SettlementTransfer> transfers = new ArrayList<>();
		int debtorIndex = 0;
		int creditorIndex = 0;
		while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
			Balance debtor = debtors.get(debtorIndex);
			Balance creditor = creditors.get(creditorIndex);
			BigDecimal amount = debtor.amount().min(creditor.amount());

			if (amount.signum() > 0) {
				transfers.add(new SettlementTransfer(debtor.userId(), creditor.userId(), money(amount)));
			}

			debtors.set(debtorIndex, new Balance(debtor.userId(), debtor.amount().subtract(amount)));
			creditors.set(creditorIndex, new Balance(creditor.userId(), creditor.amount().subtract(amount)));

			if (debtors.get(debtorIndex).amount().compareTo(BigDecimal.ZERO) == 0) {
				debtorIndex++;
			}
			if (creditors.get(creditorIndex).amount().compareTo(BigDecimal.ZERO) == 0) {
				creditorIndex++;
			}
		}

		return transfers;
	}

	private BigDecimal zero() {
		return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
	}

	private BigDecimal money(BigDecimal amount) {
		return amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
	}

	private record Balance(UUID userId, BigDecimal amount) {
	}
}
