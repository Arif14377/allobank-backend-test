package com.allobank.backendtestallobank.billgroup.dto;

import java.util.UUID;

public record MemberResponse(UUID userId, String fullName, String email) {
}
