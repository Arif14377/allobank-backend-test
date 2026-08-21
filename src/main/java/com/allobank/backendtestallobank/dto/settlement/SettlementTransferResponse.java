package com.allobank.backendtestallobank.dto.settlement;

import java.math.BigDecimal;

import com.allobank.backendtestallobank.dto.billgroup.SimpleUserResponse;

public record SettlementTransferResponse(
		SimpleUserResponse from,
		SimpleUserResponse to,
		BigDecimal amount) {
}
