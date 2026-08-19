package com.allobank.backendtestallobank.bill.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.allobank.backendtestallobank.bill.dto.BillDebtorRequest;
import com.allobank.backendtestallobank.bill.dto.BillDebtorResponse;
import com.allobank.backendtestallobank.bill.dto.BillListResponse;
import com.allobank.backendtestallobank.bill.dto.BillResponse;
import com.allobank.backendtestallobank.bill.dto.BillSummaryResponse;
import com.allobank.backendtestallobank.bill.dto.CreateBillRequest;
import com.allobank.backendtestallobank.bill.entity.BillDebtorEntity;
import com.allobank.backendtestallobank.bill.entity.BillEntity;
import com.allobank.backendtestallobank.bill.repository.BillDebtorRepository;
import com.allobank.backendtestallobank.bill.repository.BillRepository;
import com.allobank.backendtestallobank.billgroup.dto.SimpleUserResponse;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupEntity;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupMemberEntity;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupMemberRepository;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupRepository;
import com.allobank.backendtestallobank.common.error.BadRequestException;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.user.entity.UserEntity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillService {

	private final BillGroupRepository billGroupRepository;
	private final BillGroupMemberRepository billGroupMemberRepository;
	private final BillRepository billRepository;
	private final BillDebtorRepository billDebtorRepository;

	public BillService(
			BillGroupRepository billGroupRepository,
			BillGroupMemberRepository billGroupMemberRepository,
			BillRepository billRepository,
			BillDebtorRepository billDebtorRepository) {
		this.billGroupRepository = billGroupRepository;
		this.billGroupMemberRepository = billGroupMemberRepository;
		this.billRepository = billRepository;
		this.billDebtorRepository = billDebtorRepository;
	}

	@Transactional
	public BillResponse create(UUID authenticatedUserId, UUID groupId, CreateBillRequest request) {
		BillGroupEntity group = billGroupRepository.findByIdAndDeletedAtIsNull(groupId)
				.orElseThrow(() -> new ResourceNotFoundException("Bill group was not found"));

		List<BillGroupMemberEntity> memberships = billGroupMemberRepository.findActiveMembersByGroupId(groupId);
		Map<UUID, UserEntity> membersByUserId = memberships.stream()
				.map(BillGroupMemberEntity::getUser)
				.collect(Collectors.toMap(UserEntity::getId, Function.identity()));

		if (!membersByUserId.containsKey(authenticatedUserId)) {
			throw new ResourceNotFoundException("Bill group was not found");
		}

		UserEntity payer = membersByUserId.get(request.payerId());
		if (payer == null) {
			throw new BadRequestException("Payer must be a bill group member");
		}

		validateDebtors(request, membersByUserId.keySet());

		BillEntity bill = billRepository.save(BillEntity.create(
				group,
				payer,
				request.amount(),
				request.description()));
		List<BillDebtorEntity> debtors = request.debtors()
				.stream()
				.map(debtor -> BillDebtorEntity.create(bill, membersByUserId.get(debtor.userId()), debtor.amount()))
				.toList();
		billDebtorRepository.saveAll(debtors);

		return toResponse(bill, debtors);
	}

	@Transactional(readOnly = true)
	public BillListResponse listForGroup(UUID authenticatedUserId, UUID groupId) {
		billGroupRepository.findByIdAndDeletedAtIsNull(groupId)
				.orElseThrow(() -> new ResourceNotFoundException("Bill group was not found"));

		if (!billGroupMemberRepository.existsByGroup_IdAndUser_IdAndDeletedAtIsNull(groupId, authenticatedUserId)) {
			throw new ResourceNotFoundException("Bill group was not found");
		}

		List<BillSummaryResponse> bills = billRepository.findBillsByGroupId(groupId)
				.stream()
				.map(this::toSummaryResponse)
				.toList();

		return new BillListResponse(bills, bills.size());
	}

	private void validateDebtors(CreateBillRequest request, Set<UUID> memberIds) {
		Set<UUID> seenDebtorIds = new HashSet<>();
		BigDecimal allocatedAmount = BigDecimal.ZERO;

		for (BillDebtorRequest debtor : request.debtors()) {
			if (!memberIds.contains(debtor.userId())) {
				throw new BadRequestException("Debtors must be bill group members");
			}
			if (!seenDebtorIds.add(debtor.userId())) {
				throw new BadRequestException("A debtor can only appear once per bill");
			}
			allocatedAmount = allocatedAmount.add(debtor.amount());
		}

		if (allocatedAmount.compareTo(request.amount()) != 0) {
			throw new BadRequestException("Sum of debtor allocations must equal bill amount");
		}
	}

	private BillResponse toResponse(BillEntity bill, List<BillDebtorEntity> debtors) {
		List<BillDebtorResponse> debtorResponses = debtors.stream()
				.map(debtor -> new BillDebtorResponse(
						debtor.getDebtor().getId(),
						debtor.getDebtor().getFullName(),
						debtor.getAmount()))
				.toList();

		return new BillResponse(
				bill.getId(),
				new SimpleUserResponse(bill.getPayer().getId(), bill.getPayer().getFullName()),
				bill.getAmount(),
				bill.getDescription(),
				debtorResponses,
				bill.getCreatedAt());
	}

	private BillSummaryResponse toSummaryResponse(BillEntity bill) {
		return new BillSummaryResponse(
				bill.getId(),
				new SimpleUserResponse(bill.getPayer().getId(), bill.getPayer().getFullName()),
				bill.getAmount(),
				bill.getDescription(),
				bill.getCreatedAt());
	}
}
