package com.allobank.backendtestallobank.settlement.service;

import java.util.UUID;

public record SettlementParticipant(
		UUID id,
		String fullName) {
}
