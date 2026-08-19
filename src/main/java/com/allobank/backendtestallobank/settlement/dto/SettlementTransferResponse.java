package com.allobank.backendtestallobank.settlement.dto;

import java.math.BigDecimal;

import com.allobank.backendtestallobank.billgroup.dto.SimpleUserResponse;

public record SettlementTransferResponse(
		SimpleUserResponse from,
		SimpleUserResponse to,
		BigDecimal amount) {
}
