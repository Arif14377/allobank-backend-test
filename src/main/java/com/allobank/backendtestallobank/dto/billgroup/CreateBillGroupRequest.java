package com.allobank.backendtestallobank.dto.billgroup;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBillGroupRequest(
		@NotBlank
		@Size(max = 255)
		String name,

		@NotEmpty
		List<@NotNull UUID> memberIds) {
}
