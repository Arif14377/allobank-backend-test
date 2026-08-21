package com.allobank.backendtestallobank.service.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.allobank.backendtestallobank.repository.bill.BillDebtorRepository;
import com.allobank.backendtestallobank.repository.bill.BillRepository;
import com.allobank.backendtestallobank.repository.billgroup.BillGroupMemberRepository;
import com.allobank.backendtestallobank.repository.billgroup.BillGroupRepository;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.dto.settlement.SettlementResponse;
import com.allobank.backendtestallobank.entity.bill.BillDebtorEntity;
import com.allobank.backendtestallobank.entity.bill.BillEntity;
import com.allobank.backendtestallobank.entity.billgroup.BillGroupEntity;
import com.allobank.backendtestallobank.entity.billgroup.BillGroupMemberEntity;
import com.allobank.backendtestallobank.entity.user.UserEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

	private static final UUID ARIF_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID BUDI_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CITRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

	@Mock
	private BillGroupRepository billGroupRepository;

	@Mock
	private BillGroupMemberRepository billGroupMemberRepository;

	@Mock
	private BillRepository billRepository;

	@Mock
	private BillDebtorRepository billDebtorRepository;

	private final SettlementCalculator settlementCalculator = new SettlementCalculator();

	private final ServiceChargeCalculator serviceChargeCalculator = new ServiceChargeCalculator();

	private SettlementService settlementService;

	@BeforeEach
	void setUp() {
		settlementService = new SettlementService(
				billGroupRepository,
				billGroupMemberRepository,
				billRepository,
				billDebtorRepository,
				settlementCalculator,
				serviceChargeCalculator);
	}

	@Test
	void getSettlementReturnsCalculatedGroupSummaryAndTransfers() {
		UserEntity arif = new UserEntity(ARIF_ID, "Arif Rahman", "arif@example.com");
		UserEntity budi = new UserEntity(BUDI_ID, "Budi Santoso", "budi@example.com");
		UserEntity citra = new UserEntity(CITRA_ID, "Citra", "citra@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", arif);
		BillEntity bill = BillEntity.create(group, arif, new BigDecimal("300000.00"), "Lunch");

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID)).thenReturn(List.of(
				BillGroupMemberEntity.create(group, arif),
				BillGroupMemberEntity.create(group, budi),
				BillGroupMemberEntity.create(group, citra)));
		when(billRepository.findBillsByGroupId(GROUP_ID)).thenReturn(List.of(bill));
		when(billDebtorRepository.findDebtorsByGroupId(GROUP_ID)).thenReturn(List.of(
				BillDebtorEntity.create(bill, arif, new BigDecimal("100000.00")),
				BillDebtorEntity.create(bill, budi, new BigDecimal("100000.00")),
				BillDebtorEntity.create(bill, citra, new BigDecimal("100000.00"))));

		SettlementResponse response = settlementService.getSettlement(BUDI_ID, GROUP_ID);

		assertThat(response.groupId()).isEqualTo(GROUP_ID);
		assertThat(response.totalExpenses()).isEqualByComparingTo("300000.00");
		assertThat(response.serviceChargePct()).isZero();
		assertThat(response.serviceChargeAmount()).isEqualByComparingTo("0.00");
		assertThat(response.settlements()).hasSize(2);
		assertThat(response.settlements())
				.extracting(settlement -> settlement.to().id())
				.containsOnly(ARIF_ID);
	}

	@Test
	void getSettlementReturnsEmptyTransfersWhenNoBillsExist() {
		UserEntity arif = new UserEntity(ARIF_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", arif);

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(BillGroupMemberEntity.create(group, arif)));
		when(billRepository.findBillsByGroupId(GROUP_ID)).thenReturn(List.of());
		when(billDebtorRepository.findDebtorsByGroupId(GROUP_ID)).thenReturn(List.of());

		SettlementResponse response = settlementService.getSettlement(ARIF_ID, GROUP_ID);

		assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
		assertThat(response.settlements()).isEmpty();
	}

	@Test
	void getSettlementRejectsNonMemberAsNotFound() {
		UserEntity arif = new UserEntity(ARIF_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", arif);

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(BillGroupMemberEntity.create(group, arif)));

		assertThatThrownBy(() -> settlementService.getSettlement(BUDI_ID, GROUP_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Bill group was not found");

		verify(billRepository, never()).findBillsByGroupId(GROUP_ID);
		verify(billDebtorRepository, never()).findDebtorsByGroupId(GROUP_ID);
	}

	@Test
	void getSettlementRejectsMissingGroup() {
		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> settlementService.getSettlement(ARIF_ID, GROUP_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Bill group was not found");

		verifyNoMoreInteractions(billGroupMemberRepository);
		verify(billRepository, never()).findBillsByGroupId(GROUP_ID);
	}
}
