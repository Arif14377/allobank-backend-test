package com.allobank.backendtestallobank.bill.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.bill.dto.BillDebtorResponse;
import com.allobank.backendtestallobank.bill.dto.BillResponse;
import com.allobank.backendtestallobank.bill.dto.CreateBillRequest;
import com.allobank.backendtestallobank.bill.service.BillService;
import com.allobank.backendtestallobank.billgroup.controller.BillGroupController;
import com.allobank.backendtestallobank.billgroup.dto.SimpleUserResponse;
import com.allobank.backendtestallobank.billgroup.service.BillGroupService;
import com.allobank.backendtestallobank.common.error.GlobalExceptionHandler;
import com.allobank.backendtestallobank.config.security.SecurityConfig;

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
class BillControllerTest {

	private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final UUID BILL_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BillGroupService billGroupService;

	@MockitoBean
	private BillService billService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void createBillReturnsCreatedResponseContract() throws Exception {
		BillResponse response = new BillResponse(
				BILL_ID,
				new SimpleUserResponse(CREATOR_ID, "Arif Rahman"),
				new BigDecimal("300000.00"),
				"Lunch",
				List.of(
						new BillDebtorResponse(CREATOR_ID, "Arif Rahman", new BigDecimal("100000.00")),
						new BillDebtorResponse(MEMBER_ID, "Budi Santoso", new BigDecimal("200000.00"))),
				Instant.parse("2026-08-17T15:00:00Z"));
		when(billService.create(eq(CREATOR_ID), eq(GROUP_ID), any(CreateBillRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/bill-groups/{groupId}/bills", GROUP_ID)
					.with(jwt().jwt(jwt -> jwt.subject(CREATOR_ID.toString())))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "payerId": "00000000-0000-0000-0000-000000000001",
							  "amount": 300000.00,
							  "description": "Lunch",
							  "debtors": [
							    {"userId":"00000000-0000-0000-0000-000000000001","amount":100000.00},
							    {"userId":"00000000-0000-0000-0000-000000000002","amount":200000.00}
							  ]
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/bill-groups/" + GROUP_ID + "/bills/" + BILL_ID))
				.andExpect(jsonPath("$.id").value(BILL_ID.toString()))
				.andExpect(jsonPath("$.payer.id").value(CREATOR_ID.toString()))
				.andExpect(jsonPath("$.payer.fullName").value("Arif Rahman"))
				.andExpect(jsonPath("$.amount").value(300000.00))
				.andExpect(jsonPath("$.description").value("Lunch"))
				.andExpect(jsonPath("$.debtors", hasSize(2)))
				.andExpect(jsonPath("$.debtors[1].userId").value(MEMBER_ID.toString()))
				.andExpect(jsonPath("$.debtors[1].fullName").value("Budi Santoso"))
				.andExpect(jsonPath("$.debtors[1].amount").value(200000.00))
				.andExpect(jsonPath("$.createdAt").value("2026-08-17T15:00:00Z"));

		verify(billService).create(eq(CREATOR_ID), eq(GROUP_ID), any(CreateBillRequest.class));
	}

	@Test
	void createBillRejectsInvalidRequestBody() throws Exception {
		mockMvc.perform(post("/api/v1/bill-groups/{groupId}/bills", GROUP_ID)
					.with(jwt().jwt(jwt -> jwt.subject(CREATOR_ID.toString())))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "payerId": null,
							  "amount": -1,
							  "debtors": []
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors", hasSize(3)));

		verifyNoInteractions(billService);
	}

	@Test
	void createBillRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/bill-groups/{groupId}/bills", GROUP_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "payerId": "00000000-0000-0000-0000-000000000001",
							  "amount": 300000.00,
							  "debtors": [
							    {"userId":"00000000-0000-0000-0000-000000000001","amount":300000.00}
							  ]
							}
							"""))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(billService);
	}
}
