package com.allobank.backendtestallobank.service.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementDebt(
		UUID debtorId,
		BigDecimal amount) {
}
