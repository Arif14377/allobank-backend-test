package com.allobank.backendtestallobank.settlement.service;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementTransfer(
		UUID fromUserId,
		UUID toUserId,
		BigDecimal amount) {
}
