package com.allobank.backendtestallobank.service.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementBill(
		UUID payerId,
		BigDecimal amount) {
}
