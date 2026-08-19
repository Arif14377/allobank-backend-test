package com.allobank.backendtestallobank.billgroup.controller;

import java.net.URI;
import java.util.UUID;

import com.allobank.backendtestallobank.billgroup.service.BillGroupService;
import com.allobank.backendtestallobank.billgroup.dto.BillGroupListResponse;
import com.allobank.backendtestallobank.billgroup.dto.BillGroupResponse;
import com.allobank.backendtestallobank.billgroup.dto.CreateBillGroupRequest;
import com.allobank.backendtestallobank.common.error.BadRequestException;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bill-groups")
public class BillGroupController {

	private final BillGroupService billGroupService;

	public BillGroupController(BillGroupService billGroupService) {
		this.billGroupService = billGroupService;
	}

	@PostMapping
	ResponseEntity<BillGroupResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateBillGroupRequest request) {
		UUID authenticatedUserId = authenticatedUserId(jwt);
		BillGroupResponse response = billGroupService.create(authenticatedUserId, request);
		return ResponseEntity
				.created(URI.create("/api/v1/bill-groups/" + response.id()))
				.body(response);
	}

	@GetMapping
	ResponseEntity<BillGroupListResponse> list(@AuthenticationPrincipal Jwt jwt) {
		UUID authenticatedUserId = authenticatedUserId(jwt);
		return ResponseEntity.ok(billGroupService.listForUser(authenticatedUserId));
	}

	@GetMapping("/{groupId}")
	ResponseEntity<BillGroupResponse> detail(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID groupId) {
		UUID authenticatedUserId = authenticatedUserId(jwt);
		return ResponseEntity.ok(billGroupService.getDetail(authenticatedUserId, groupId));
	}

	private UUID authenticatedUserId(Jwt jwt) {
		try {
			return UUID.fromString(jwt.getSubject());
		}
		catch (IllegalArgumentException exception) {
			throw new BadRequestException("JWT subject must be a valid user UUID");
		}
	}
}
