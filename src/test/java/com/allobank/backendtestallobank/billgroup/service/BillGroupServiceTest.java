package com.allobank.backendtestallobank.billgroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.allobank.backendtestallobank.billgroup.entity.BillGroupEntity;
import com.allobank.backendtestallobank.billgroup.entity.BillGroupMemberEntity;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupMemberRepository;
import com.allobank.backendtestallobank.billgroup.repository.BillGroupRepository;
import com.allobank.backendtestallobank.billgroup.dto.BillGroupListResponse;
import com.allobank.backendtestallobank.billgroup.dto.BillGroupResponse;
import com.allobank.backendtestallobank.billgroup.dto.CreateBillGroupRequest;
import com.allobank.backendtestallobank.common.error.ResourceNotFoundException;
import com.allobank.backendtestallobank.user.entity.UserEntity;
import com.allobank.backendtestallobank.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillGroupServiceTest {

	private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

	@Mock
	private BillGroupRepository billGroupRepository;

	@Mock
	private BillGroupMemberRepository billGroupMemberRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private BillGroupService billGroupService;

	@Test
	void createUsesAuthenticatedUserAsCreatorAndMember() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		UserEntity member = new UserEntity(MEMBER_ID, "Budi Santoso", "budi@example.com");
		CreateBillGroupRequest request = new CreateBillGroupRequest(" Trip Bandung ", List.of(MEMBER_ID));

		when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(creator));
		when(userRepository.findAllById(List.of(CREATOR_ID, MEMBER_ID))).thenReturn(List.of(creator, member));
		when(billGroupRepository.save(any(BillGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BillGroupResponse response = billGroupService.create(CREATOR_ID, request);

		ArgumentCaptor<BillGroupEntity> groupCaptor = ArgumentCaptor.forClass(BillGroupEntity.class);
		verify(billGroupRepository).save(groupCaptor.capture());
		assertThat(groupCaptor.getValue().getName()).isEqualTo("Trip Bandung");
		assertThat(groupCaptor.getValue().getCreatedBy().getId()).isEqualTo(CREATOR_ID);

		ArgumentCaptor<List<BillGroupMemberEntity>> membersCaptor = ArgumentCaptor.forClass(List.class);
		verify(billGroupMemberRepository).saveAll(membersCaptor.capture());
		assertThat(membersCaptor.getValue())
				.extracting(memberEntity -> memberEntity.getUser().getId())
				.containsExactly(CREATOR_ID, MEMBER_ID);

		assertThat(response.name()).isEqualTo("Trip Bandung");
		assertThat(response.createdBy().id()).isEqualTo(CREATOR_ID);
		assertThat(response.members())
				.extracting(memberResponse -> memberResponse.userId())
				.containsExactly(CREATOR_ID, MEMBER_ID);
	}

	@Test
	void listForUserReturnsOnlyGroupsWhereUserIsMember() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);

		when(billGroupRepository.findActiveGroupsByMemberUserId(MEMBER_ID)).thenReturn(List.of(group));
		when(billGroupMemberRepository.countByGroup_IdAndDeletedAtIsNull(group.getId())).thenReturn(2L);

		BillGroupListResponse response = billGroupService.listForUser(MEMBER_ID);

		assertThat(response.total()).isEqualTo(1);
		assertThat(response.data()).hasSize(1);
		assertThat(response.data().get(0).id()).isEqualTo(group.getId());
		assertThat(response.data().get(0).name()).isEqualTo("Trip Bandung");
		assertThat(response.data().get(0).createdBy().id()).isEqualTo(CREATOR_ID);
		assertThat(response.data().get(0).memberCount()).isEqualTo(2);

		verify(billGroupRepository).findActiveGroupsByMemberUserId(MEMBER_ID);
		verify(billGroupMemberRepository).countByGroup_IdAndDeletedAtIsNull(group.getId());
	}

	@Test
	void getDetailReturnsGroupWhenAuthenticatedUserIsMember() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		UserEntity member = new UserEntity(MEMBER_ID, "Budi Santoso", "budi@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);
		BillGroupMemberEntity creatorMembership = BillGroupMemberEntity.create(group, creator);
		BillGroupMemberEntity memberMembership = BillGroupMemberEntity.create(group, member);

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.existsByGroup_IdAndUser_IdAndDeletedAtIsNull(GROUP_ID, MEMBER_ID))
				.thenReturn(true);
		when(billGroupMemberRepository.findActiveMembersByGroupId(GROUP_ID))
				.thenReturn(List.of(creatorMembership, memberMembership));

		BillGroupResponse response = billGroupService.getDetail(MEMBER_ID, GROUP_ID);

		assertThat(response.id()).isEqualTo(group.getId());
		assertThat(response.name()).isEqualTo("Trip Bandung");
		assertThat(response.createdBy().id()).isEqualTo(CREATOR_ID);
		assertThat(response.members())
				.extracting(memberResponse -> memberResponse.userId())
				.containsExactly(CREATOR_ID, MEMBER_ID);
	}

	@Test
	void getDetailRejectsMissingGroup() {
		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> billGroupService.getDetail(MEMBER_ID, GROUP_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Bill group was not found");

		verify(billGroupRepository).findByIdAndDeletedAtIsNull(GROUP_ID);
		verifyNoMoreInteractions(billGroupMemberRepository);
	}

	@Test
	void getDetailRejectsNonMemberAccess() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		BillGroupEntity group = BillGroupEntity.create("Trip Bandung", creator);

		when(billGroupRepository.findByIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(Optional.of(group));
		when(billGroupMemberRepository.existsByGroup_IdAndUser_IdAndDeletedAtIsNull(GROUP_ID, MEMBER_ID))
				.thenReturn(false);

		assertThatThrownBy(() -> billGroupService.getDetail(MEMBER_ID, GROUP_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Bill group was not found");

		verify(billGroupMemberRepository, never()).findActiveMembersByGroupId(GROUP_ID);
	}

	@Test
	void createRejectsMissingMember() {
		UserEntity creator = new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com");
		CreateBillGroupRequest request = new CreateBillGroupRequest("Trip Bandung", List.of(MEMBER_ID));

		when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(creator));
		when(userRepository.findAllById(List.of(CREATOR_ID, MEMBER_ID))).thenReturn(List.of(creator));

		assertThatThrownBy(() -> billGroupService.create(CREATOR_ID, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(MEMBER_ID.toString());

		verify(billGroupRepository, never()).save(any(BillGroupEntity.class));
		verify(billGroupMemberRepository, never()).saveAll(anyList());
	}
}
