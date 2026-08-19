package com.allobank.backendtestallobank.billgroup.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.bill.service.BillService;
import com.allobank.backendtestallobank.billgroup.service.BillGroupService;
import com.allobank.backendtestallobank.billgroup.dto.BillGroupListResponse;
import com.allobank.backendtestallobank.billgroup.dto.BillGroupResponse;
import com.allobank.backendtestallobank.billgroup.dto.BillGroupSummaryResponse;
import com.allobank.backendtestallobank.billgroup.dto.CreateBillGroupRequest;
import com.allobank.backendtestallobank.billgroup.dto.MemberResponse;
import com.allobank.backendtestallobank.billgroup.dto.SimpleUserResponse;
import com.allobank.backendtestallobank.common.error.GlobalExceptionHandler;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.config.security.SecurityConfig;
import com.allobank.backendtestallobank.settlement.service.SettlementService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillGroupController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
@ImportAutoConfiguration({ SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class })
class BillGroupControllerTest {

	private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BillGroupService billGroupService;

	@MockitoBean
	private BillService billService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private SettlementService settlementService;

	@Test
	void createReturnsCreatedResponseContract() throws Exception {
		BillGroupResponse response = new BillGroupResponse(
				GROUP_ID,
				"Trip Bandung",
				new SimpleUserResponse(CREATOR_ID, "Arif Rahman"),
				List.of(
						new MemberResponse(CREATOR_ID, "Arif Rahman", "arif@example.com"),
						new MemberResponse(MEMBER_ID, "Budi Santoso", "budi@example.com")),
				Instant.parse("2026-08-16T10:00:00Z"));
		when(billGroupService.create(eq(CREATOR_ID), any(CreateBillGroupRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/bill-groups")
					.with(jwt().jwt(jwt -> jwt.subject(CREATOR_ID.toString())))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Trip Bandung",
							  "memberIds": ["00000000-0000-0000-0000-000000000002"]
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/bill-groups/" + GROUP_ID))
				.andExpect(jsonPath("$.id").value(GROUP_ID.toString()))
				.andExpect(jsonPath("$.name").value("Trip Bandung"))
				.andExpect(jsonPath("$.createdBy.id").value(CREATOR_ID.toString()))
				.andExpect(jsonPath("$.createdBy.fullName").value("Arif Rahman"))
				.andExpect(jsonPath("$.members", hasSize(2)))
				.andExpect(jsonPath("$.members[1].userId").value(MEMBER_ID.toString()))
				.andExpect(jsonPath("$.createdAt").value("2026-08-16T10:00:00Z"));

		verify(billGroupService).create(eq(CREATOR_ID), any(CreateBillGroupRequest.class));
	}

	@Test
	void listReturnsGroupsForAuthenticatedUser() throws Exception {
		BillGroupListResponse response = new BillGroupListResponse(
				List.of(new BillGroupSummaryResponse(
						GROUP_ID,
						"Trip Bandung",
						new SimpleUserResponse(CREATOR_ID, "Arif Rahman"),
						2,
						Instant.parse("2026-08-16T10:00:00Z"))),
				1);
		when(billGroupService.listForUser(CREATOR_ID)).thenReturn(response);

		mockMvc.perform(get("/api/v1/bill-groups")
					.with(jwt().jwt(jwt -> jwt.subject(CREATOR_ID.toString())))
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].id").value(GROUP_ID.toString()))
				.andExpect(jsonPath("$.data[0].name").value("Trip Bandung"))
				.andExpect(jsonPath("$.data[0].createdBy.id").value(CREATOR_ID.toString()))
				.andExpect(jsonPath("$.data[0].createdBy.fullName").value("Arif Rahman"))
				.andExpect(jsonPath("$.data[0].memberCount").value(2))
				.andExpect(jsonPath("$.data[0].createdAt").value("2026-08-16T10:00:00Z"))
				.andExpect(jsonPath("$.total").value(1));

		verify(billGroupService).listForUser(CREATOR_ID);
	}

	@Test
	void listRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/bill-groups")
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(billGroupService);
	}

	@Test
	void detailReturnsGroupForAuthenticatedMember() throws Exception {
		BillGroupResponse response = new BillGroupResponse(
				GROUP_ID,
				"Trip Bandung",
				new SimpleUserResponse(CREATOR_ID, "Arif Rahman"),
				List.of(
						new MemberResponse(CREATOR_ID, "Arif Rahman", "arif@example.com"),
						new MemberResponse(MEMBER_ID, "Budi Santoso", "budi@example.com")),
				Instant.parse("2026-08-16T10:00:00Z"));
		when(billGroupService.getDetail(CREATOR_ID, GROUP_ID)).thenReturn(response);

		mockMvc.perform(get("/api/v1/bill-groups/{groupId}", GROUP_ID)
					.with(jwt().jwt(jwt -> jwt.subject(CREATOR_ID.toString())))
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(GROUP_ID.toString()))
				.andExpect(jsonPath("$.name").value("Trip Bandung"))
				.andExpect(jsonPath("$.createdBy.id").value(CREATOR_ID.toString()))
				.andExpect(jsonPath("$.members", hasSize(2)))
				.andExpect(jsonPath("$.members[0].userId").value(CREATOR_ID.toString()))
				.andExpect(jsonPath("$.members[0].email").value("arif@example.com"))
				.andExpect(jsonPath("$.createdAt").value("2026-08-16T10:00:00Z"));

		verify(billGroupService).getDetail(CREATOR_ID, GROUP_ID);
	}

	@Test
	void detailReturnsNotFoundForMissingOrInaccessibleGroup() throws Exception {
		when(billGroupService.getDetail(CREATOR_ID, GROUP_ID))
				.thenThrow(new ResourceNotFoundException("Bill group was not found"));

		mockMvc.perform(get("/api/v1/bill-groups/{groupId}", GROUP_ID)
					.with(jwt().jwt(jwt -> jwt.subject(CREATOR_ID.toString())))
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Bill group was not found"));
	}

	@Test
	void detailRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/bill-groups/{groupId}", GROUP_ID)
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(billGroupService);
	}

	@Test
	void createRejectsInvalidRequestBody() throws Exception {
		mockMvc.perform(post("/api/v1/bill-groups")
					.with(jwt().jwt(jwt -> jwt.subject(CREATOR_ID.toString())))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "",
							  "memberIds": []
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(2)));

		verifyNoInteractions(billGroupService);
	}

	@Test
	void createRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/bill-groups")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Trip Bandung",
							  "memberIds": ["00000000-0000-0000-0000-000000000002"]
							}
							"""))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(billGroupService);
	}
}
