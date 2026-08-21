package com.allobank.backendtestallobank.controller.settlement;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.service.bill.BillService;
import com.allobank.backendtestallobank.controller.billgroup.BillGroupController;
import com.allobank.backendtestallobank.dto.billgroup.SimpleUserResponse;
import com.allobank.backendtestallobank.dto.settlement.SettlementResponse;
import com.allobank.backendtestallobank.dto.settlement.SettlementTransferResponse;
import com.allobank.backendtestallobank.service.billgroup.BillGroupService;
import com.allobank.backendtestallobank.common.error.GlobalExceptionHandler;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.config.security.SecurityConfig;
import com.allobank.backendtestallobank.service.settlement.SettlementService;

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
class SettlementControllerTest {

	private static final UUID ARIF_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID BUDI_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BillGroupService billGroupService;

	@MockitoBean
	private BillService billService;

	@MockitoBean
	private SettlementService settlementService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void settlementReturnsOkResponseContract() throws Exception {
		SettlementResponse response = new SettlementResponse(
				GROUP_ID,
				new BigDecimal("300000.00"),
				0,
				new BigDecimal("0.00"),
				List.of(new SettlementTransferResponse(
						new SimpleUserResponse(BUDI_ID, "Budi Santoso"),
						new SimpleUserResponse(ARIF_ID, "Arif Rahman"),
						new BigDecimal("100000.00"))));
		when(settlementService.getSettlement(ARIF_ID, GROUP_ID)).thenReturn(response);

		mockMvc.perform(get("/api/v1/bill-groups/{groupId}/settlement", GROUP_ID)
					.with(jwt().jwt(jwt -> jwt.subject(ARIF_ID.toString())))
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
				.andExpect(jsonPath("$.totalExpenses").value(300000.00))
				.andExpect(jsonPath("$.serviceChargePct").value(0))
				.andExpect(jsonPath("$.serviceChargeAmount").value(0.00))
				.andExpect(jsonPath("$.settlements", hasSize(1)))
				.andExpect(jsonPath("$.settlements[0].from.id").value(BUDI_ID.toString()))
				.andExpect(jsonPath("$.settlements[0].from.fullName").value("Budi Santoso"))
				.andExpect(jsonPath("$.settlements[0].to.id").value(ARIF_ID.toString()))
				.andExpect(jsonPath("$.settlements[0].to.fullName").value("Arif Rahman"))
				.andExpect(jsonPath("$.settlements[0].amount").value(100000.00));

		verify(settlementService).getSettlement(ARIF_ID, GROUP_ID);
	}

	@Test
	void settlementHidesGroupFromNonMember() throws Exception {
		when(settlementService.getSettlement(BUDI_ID, GROUP_ID))
				.thenThrow(new ResourceNotFoundException("Bill group was not found"));

		mockMvc.perform(get("/api/v1/bill-groups/{groupId}/settlement", GROUP_ID)
					.with(jwt().jwt(jwt -> jwt.subject(BUDI_ID.toString())))
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Bill group was not found"));

		verify(settlementService).getSettlement(BUDI_ID, GROUP_ID);
	}

	@Test
	void settlementRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/bill-groups/{groupId}/settlement", GROUP_ID)
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(settlementService);
	}
}
