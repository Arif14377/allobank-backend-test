package com.allobank.backendtestallobank.service.billgroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.allobank.backendtestallobank.repository.billgroup.BillGroupMemberRepository;
import com.allobank.backendtestallobank.repository.billgroup.BillGroupRepository;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.dto.billgroup.BillGroupListResponse;
import com.allobank.backendtestallobank.dto.billgroup.BillGroupResponse;
import com.allobank.backendtestallobank.dto.billgroup.BillGroupSummaryResponse;
import com.allobank.backendtestallobank.dto.billgroup.CreateBillGroupRequest;
import com.allobank.backendtestallobank.dto.billgroup.MemberResponse;
import com.allobank.backendtestallobank.dto.billgroup.SimpleUserResponse;
import com.allobank.backendtestallobank.entity.billgroup.BillGroupEntity;
import com.allobank.backendtestallobank.entity.billgroup.BillGroupMemberEntity;
import com.allobank.backendtestallobank.entity.user.UserEntity;
import com.allobank.backendtestallobank.repository.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillGroupService {

	private final BillGroupRepository billGroupRepository;
	private final BillGroupMemberRepository billGroupMemberRepository;
	private final UserRepository userRepository;

	public BillGroupService(
			BillGroupRepository billGroupRepository,
			BillGroupMemberRepository billGroupMemberRepository,
			UserRepository userRepository) {
		this.billGroupRepository = billGroupRepository;
		this.billGroupMemberRepository = billGroupMemberRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public BillGroupResponse create(UUID authenticatedUserId, CreateBillGroupRequest request) {
		UserEntity creator = userRepository.findById(authenticatedUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));

		List<UUID> orderedMemberIds = buildOrderedMemberIds(authenticatedUserId, request.memberIds());
		Map<UUID, UserEntity> usersById = userRepository.findAllById(orderedMemberIds)
				.stream()
				.collect(Collectors.toMap(UserEntity::getId, Function.identity()));

		List<UUID> missingMemberIds = orderedMemberIds.stream()
				.filter(memberId -> !usersById.containsKey(memberId))
				.toList();
		if (!missingMemberIds.isEmpty()) {
			throw new ResourceNotFoundException("One or more group members were not found: " + missingMemberIds);
		}

		BillGroupEntity group = billGroupRepository.save(BillGroupEntity.create(request.name().trim(), creator));
		List<UserEntity> orderedUsers = orderedMemberIds.stream()
				.map(usersById::get)
				.toList();
		List<BillGroupMemberEntity> members = orderedUsers.stream()
				.map(user -> BillGroupMemberEntity.create(group, user))
				.toList();
		billGroupMemberRepository.saveAll(members);

		return toResponse(group, orderedUsers);
	}

	@Transactional(readOnly = true)
	public BillGroupListResponse listForUser(UUID authenticatedUserId) {
		List<BillGroupSummaryResponse> groups = billGroupRepository.findActiveGroupsByMemberUserId(authenticatedUserId)
				.stream()
				.map(this::toSummaryResponse)
				.toList();

		return new BillGroupListResponse(groups, groups.size());
	}

	@Transactional(readOnly = true)
	public BillGroupResponse getDetail(UUID authenticatedUserId, UUID groupId) {
		BillGroupEntity group = billGroupRepository.findByIdAndDeletedAtIsNull(groupId)
				.orElseThrow(() -> new ResourceNotFoundException("Bill group was not found"));

		if (!billGroupMemberRepository.existsByGroup_IdAndUser_IdAndDeletedAtIsNull(groupId, authenticatedUserId)) {
			throw new ResourceNotFoundException("Bill group was not found");
		}

		List<UserEntity> members = billGroupMemberRepository.findActiveMembersByGroupId(groupId)
				.stream()
				.map(BillGroupMemberEntity::getUser)
				.toList();

		return toResponse(group, members);
	}

	private List<UUID> buildOrderedMemberIds(UUID authenticatedUserId, List<UUID> requestMemberIds) {
		Set<UUID> uniqueIds = new LinkedHashSet<>();
		uniqueIds.add(authenticatedUserId);
		uniqueIds.addAll(requestMemberIds);
		return new ArrayList<>(uniqueIds);
	}

	private BillGroupSummaryResponse toSummaryResponse(BillGroupEntity group) {
		return new BillGroupSummaryResponse(
				group.getId(),
				group.getName(),
				new SimpleUserResponse(group.getCreatedBy().getId(), group.getCreatedBy().getFullName()),
				billGroupMemberRepository.countByGroup_IdAndDeletedAtIsNull(group.getId()),
				group.getCreatedAt());
	}

	private BillGroupResponse toResponse(BillGroupEntity group, List<UserEntity> members) {
		List<MemberResponse> memberResponses = members.stream()
				.map(member -> new MemberResponse(member.getId(), member.getFullName(), member.getEmail()))
				.toList();
		return new BillGroupResponse(
				group.getId(),
				group.getName(),
				new SimpleUserResponse(group.getCreatedBy().getId(), group.getCreatedBy().getFullName()),
				memberResponses,
				group.getCreatedAt());
	}
}
