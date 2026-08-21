package com.allobank.backendtestallobank.service.settlement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SettlementResult(
		BigDecimal totalExpenses,
		Map<UUID, BigDecimal> balances,
		List<SettlementTransfer> transfers) {
}
