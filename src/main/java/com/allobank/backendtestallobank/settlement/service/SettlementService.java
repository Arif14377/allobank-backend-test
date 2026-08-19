package com.allobank.backendtestallobank.settlement.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.allobank.backendtestallobank.bill.entity.BillDebtorEntity;
import com.allobank.backendtestallobank.bill.entity.BillEntity;
import com.allobank.backendtestallobank.bill.repository.BillDebtorRepository;
import com.allobank.backendtestallobank.bill.repository.BillRepository;
import com.allobank.backendtestallobank.billgroup.dto.SimpleUserResponse;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupMemberEntity;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupMemberRepository;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupRepository;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.settlement.dto.SettlementResponse;
import com.allobank.backendtestallobank.settlement.dto.SettlementTransferResponse;
import com.allobank.backendtestallobank.user.entity.UserEntity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

	private final BillGroupRepository billGroupRepository;
	private final BillGroupMemberRepository billGroupMemberRepository;
	private final BillRepository billRepository;
	private final BillDebtorRepository billDebtorRepository;
	private final SettlementCalculator settlementCalculator;
	private final ServiceChargeCalculator serviceChargeCalculator;

	public SettlementService(
			BillGroupRepository billGroupRepository,
			BillGroupMemberRepository billGroupMemberRepository,
			BillRepository billRepository,
			BillDebtorRepository billDebtorRepository,
			SettlementCalculator settlementCalculator,
			ServiceChargeCalculator serviceChargeCalculator) {
		this.billGroupRepository = billGroupRepository;
		this.billGroupMemberRepository = billGroupMemberRepository;
		this.billRepository = billRepository;
		this.billDebtorRepository = billDebtorRepository;
		this.settlementCalculator = settlementCalculator;
		this.serviceChargeCalculator = serviceChargeCalculator;
	}

	@Transactional(readOnly = true)
	public SettlementResponse getSettlement(UUID authenticatedUserId, UUID groupId) {
		billGroupRepository.findByIdAndDeletedAtIsNull(groupId)
				.orElseThrow(() -> new ResourceNotFoundException("Bill group was not found"));

		List<BillGroupMemberEntity> memberships = billGroupMemberRepository.findActiveMembersByGroupId(groupId);
		Map<UUID, UserEntity> membersById = memberships.stream()
				.map(BillGroupMemberEntity::getUser)
				.collect(Collectors.toMap(UserEntity::getId, Function.identity()));

		if (!membersById.containsKey(authenticatedUserId)) {
			throw new ResourceNotFoundException("Bill group was not found");
		}

		List<BillEntity> bills = billRepository.findBillsByGroupId(groupId);
		List<BillDebtorEntity> debtors = billDebtorRepository.findDebtorsByGroupId(groupId);

		SettlementResult result = settlementCalculator.calculate(
				membersById.values().stream()
						.map(member -> new SettlementParticipant(member.getId(), member.getFullName()))
						.toList(),
				bills.stream()
						.map(bill -> new SettlementBill(bill.getPayer().getId(), bill.getAmount()))
						.toList(),
				debtors.stream()
						.map(debtor -> new SettlementDebt(debtor.getDebtor().getId(), debtor.getAmount()))
						.toList());

		List<SettlementTransferResponse> settlements = result.transfers()
				.stream()
				.map(transfer -> toResponse(transfer, membersById))
				.toList();

		return new SettlementResponse(
				groupId,
				result.totalExpenses(),
				serviceChargeCalculator.percentage(),
				serviceChargeCalculator.calculateAmount(result.totalExpenses()),
				settlements);
	}

	private SettlementTransferResponse toResponse(SettlementTransfer transfer, Map<UUID, UserEntity> membersById) {
		UserEntity from = membersById.get(transfer.fromUserId());
		UserEntity to = membersById.get(transfer.toUserId());
		return new SettlementTransferResponse(
				new SimpleUserResponse(from.getId(), from.getFullName()),
				new SimpleUserResponse(to.getId(), to.getFullName()),
				transfer.amount());
	}
}
