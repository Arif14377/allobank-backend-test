package com.allobank.backendtestallobank.bill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.allobank.backendtestallobank.bill.dto.BillDebtorRequest;
import com.allobank.backendtestallobank.bill.dto.BillResponse;
import com.allobank.backendtestallobank.bill.dto.CreateBillRequest;
import com.allobank.backendtestallobank.bill.entity.BillDebtorEntity;
import com.allobank.backendtestallobank.bill.entity.BillEntity;
import com.allobank.backendtestallobank.bill.repository.BillDebtorRepository;
import com.allobank.backendtestallobank.bill.repository.BillRepository;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupEntity;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupMemberEntity;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupMemberRepository;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupRepository;
import com.allobank.backendtestallobank.common.error.BadRequestException;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.user.entity.UserEntity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

	private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

	@Mock
	private BillGroupRepository billGroupRepository;

	@Mock
	private BillGroupMemberRepository billGroupMemberRepository;

	@Mock
	private BillRepository billRepository;

	@Mock
	private BillDebtorRepository billDebtorRepository;

	@InjectMocks
	private BillService billService;

	@Test
	void createPersistsBillAndDebtorsWhenAllocationMatchesAmount() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		UserEntity member = new UserEntity(MEMBER_ID, "Budi Santoso", "budi@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);
		CreateBillRequest request = new CreateBillRequest(
				CREATOR_ID,
				new BigDecimal("300000.00"),
				" Lunch ",
				List.of(
						new BillDebtorRequest(CREATOR_ID, new BigDecimal("100000.00")),
						new BillDebtorRequest(MEMBER_ID, new BigDecimal("200000.00"))));

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID)).thenReturn(List.of(
				BillGroupMemberEntity.create(group, creator),
				BillGroupMemberEntity.create(group, member)));
		when(billRepository.save(any(BillEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BillResponse response = billService.create(CREATOR_ID, GROUP_ID, request);

		ArgumentCaptor<BillEntity> billCaptor = ArgumentCaptor.forClass(BillEntity.class);
		verify(billRepository).save(billCaptor.capture());
		assertThat(billCaptor.getValue().getGroup()).isSameAs(group);
		assertThat(billCaptor.getValue().getPayer().getId()).isEqualTo(CREATOR_ID);
		assertThat(billCaptor.getValue().getAmount()).isEqualByComparingTo("300000.00");
		assertThat(billCaptor.getValue().getDescription()).isEqualTo("Lunch");

		ArgumentCaptor<List<BillDebtorEntity>> debtorsCaptor = ArgumentCaptor.forClass(List.class);
		verify(billDebtorRepository).saveAll(debtorsCaptor.capture());
		assertThat(debtorsCaptor.getValue())
				.extracting(debtor -> debtor.getDebtor().getId())
				.containsExactly(CREATOR_ID, MEMBER_ID);
		assertThat(debtorsCaptor.getValue())
				.extracting(BillDebtorEntity::getAmount)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(new BigDecimal("100000.00"), new BigDecimal("200000.00"));

		assertThat(response.payer().id()).isEqualTo(CREATOR_ID);
		assertThat(response.amount()).isEqualByComparingTo("300000.00");
		assertThat(response.description()).isEqualTo("Lunch");
		assertThat(response.debtors()).hasSize(2);
	}

	@Test
	void createRejectsNonMemberAuthenticatedUserAsNotFound() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(BillGroupMemberEntity.create(group, creator)));

		assertThatThrownBy(() -> billService.create(MEMBER_ID, GROUP_ID, validRequest()))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Bill group was not found");

		verify(billRepository, never()).save(any(BillEntity.class));
		verify(billDebtorRepository, never()).saveAll(anyList());
	}

	@Test
	void createRejectsPayerOutsideGroup() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);
		CreateBillRequest request = new CreateBillRequest(
				OTHER_USER_ID,
				new BigDecimal("300000.00"),
				"Lunch",
				List.of(new BillDebtorRequest(CREATOR_ID, new BigDecimal("300000.00"))));

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(BillGroupMemberEntity.create(group, creator)));

		assertThatThrownBy(() -> billService.create(CREATOR_ID, GROUP_ID, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Payer must be a bill group member");

		verify(billRepository, never()).save(any(BillEntity.class));
	}

	@Test
	void createRejectsDebtorOutsideGroup() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);
		CreateBillRequest request = new CreateBillRequest(
				CREATOR_ID,
				new BigDecimal("300000.00"),
				"Lunch",
				List.of(new BillDebtorRequest(OTHER_USER_ID, new BigDecimal("300000.00"))));

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(BillGroupMemberEntity.create(group, creator)));

		assertThatThrownBy(() -> billService.create(CREATOR_ID, GROUP_ID, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Debtors must be bill group members");

		verify(billRepository, never()).save(any(BillEntity.class));
	}

	@Test
	void createRejectsDuplicateDebtor() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);
		CreateBillRequest request = new CreateBillRequest(
				CREATOR_ID,
				new BigDecimal("300000.00"),
				"Lunch",
				List.of(
						new BillDebtorRequest(CREATOR_ID, new BigDecimal("100000.00")),
						new BillDebtorRequest(CREATOR_ID, new BigDecimal("200000.00"))));

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(BillGroupMemberEntity.create(group, creator)));

		assertThatThrownBy(() -> billService.create(CREATOR_ID, GROUP_ID, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("A debtor can only appear once per bill");

		verify(billRepository, never()).save(any(BillEntity.class));
	}

	@Test
	void createRejectsAllocationThatDoesNotEqualBillAmount() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);
		CreateBillRequest request = new CreateBillRequest(
				CREATOR_ID,
				new BigDecimal("300000.00"),
				"Lunch",
				List.of(new BillDebtorRequest(CREATOR_ID, new BigDecimal("250000.00"))));

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(BillGroupMemberEntity.create(group, creator)));

		assertThatThrownBy(() -> billService.create(CREATOR_ID, GROUP_ID, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Sum of debtor allocations must equal bill amount");

		verify(billRepository, never()).save(any(BillEntity.class));
	}

	@Test
	void createRejectsMissingGroup() {
		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> billService.create(CREATOR_ID, GROUP_ID, validRequest()))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Bill group was not found");

		verifyNoMoreInteractions(billGroupMemberRepository);
		verify(billRepository, never()).save(any(BillEntity.class));
	}

	private CreateBillRequest validRequest() {
		return new CreateBillRequest(
				CREATOR_ID,
				new BigDecimal("300000.00"),
				"Lunch",
				List.of(new BillDebtorRequest(CREATOR_ID, new BigDecimal("300000.00"))));
	}
}
