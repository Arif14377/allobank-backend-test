package com.allobank.backendtestallobank.settlement.service;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementDebt(
		UUID debtorId,
		BigDecimal amount) {
}
