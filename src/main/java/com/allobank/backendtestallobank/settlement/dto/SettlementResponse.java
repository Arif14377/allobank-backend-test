package com.allobank.backendtestallobank.settlement.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SettlementResponse(
		UUID groupId,
		BigDecimal totalExpenses,
		int serviceChargePct,
		BigDecimal serviceChargeAmount,
		List<SettlementTransferResponse> settlements) {
}
