package com.allobank.backendtestallobank.settlement.service;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementBill(
		UUID payerId,
		BigDecimal amount) {
}
