package com.allobank.backendtestallobank.dto.billgroup;

import java.util.UUID;

public record MemberResponse(UUID userId, String fullName, String email) {
}
