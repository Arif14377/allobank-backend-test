package com.allobank.backendtestallobank.service.settlement;

import java.util.UUID;

public record SettlementParticipant(
		UUID id,
		String fullName) {
}
