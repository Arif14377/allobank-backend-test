package com.allobank.backendtestallobank.service.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementTransfer(
		UUID fromUserId,
		UUID toUserId,
		BigDecimal amount) {
}
