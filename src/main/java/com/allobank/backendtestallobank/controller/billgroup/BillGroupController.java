package com.allobank.backendtestallobank.controller.billgroup;

import java.net.URI;
import java.util.UUID;

import com.allobank.backendtestallobank.service.bill.BillService;
import com.allobank.backendtestallobank.service.billgroup.BillGroupService;
import com.allobank.backendtestallobank.common.error.BadRequestException;
import com.allobank.backendtestallobank.dto.bill.BillListResponse;
import com.allobank.backendtestallobank.dto.bill.BillResponse;
import com.allobank.backendtestallobank.dto.bill.CreateBillRequest;
import com.allobank.backendtestallobank.dto.billgroup.BillGroupListResponse;
import com.allobank.backendtestallobank.dto.billgroup.BillGroupResponse;
import com.allobank.backendtestallobank.dto.billgroup.CreateBillGroupRequest;
import com.allobank.backendtestallobank.dto.settlement.SettlementResponse;
import com.allobank.backendtestallobank.service.settlement.SettlementService;

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
	private final BillService billService;
	private final SettlementService settlementService;

	public BillGroupController(
			BillGroupService billGroupService,
			BillService billService,
			SettlementService settlementService) {
		this.billGroupService = billGroupService;
		this.billService = billService;
		this.settlementService = settlementService;
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

	@PostMapping("/{groupId}/bills")
	ResponseEntity<BillResponse> createBill(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID groupId,
			@Valid @RequestBody CreateBillRequest request) {
		UUID authenticatedUserId = authenticatedUserId(jwt);
		BillResponse response = billService.create(authenticatedUserId, groupId, request);
		return ResponseEntity
				.created(URI.create("/api/v1/bill-groups/" + groupId + "/bills/" + response.id()))
				.body(response);
	}

	@GetMapping("/{groupId}/bills")
	ResponseEntity<BillListResponse> listBills(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID groupId) {
		UUID authenticatedUserId = authenticatedUserId(jwt);
		return ResponseEntity.ok(billService.listForGroup(authenticatedUserId, groupId));
	}

	@GetMapping("/{groupId}/settlement")
	ResponseEntity<SettlementResponse> settlement(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID groupId) {
		UUID authenticatedUserId = authenticatedUserId(jwt);
		return ResponseEntity.ok(settlementService.getSettlement(authenticatedUserId, groupId));
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
