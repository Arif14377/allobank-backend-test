package com.allobank.backendtestallobank.repository.billgroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.entity.billgroup.BillGroupEntity;
import com.allobank.backendtestallobank.entity.billgroup.BillGroupMemberEntity;
import com.allobank.backendtestallobank.entity.user.UserEntity;
import com.allobank.backendtestallobank.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class BillGroupPersistenceTest {

	private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BillGroupRepository billGroupRepository;

	@Autowired
	private BillGroupMemberRepository billGroupMemberRepository;

	@Test
	void persistsGroupCreatorAndMembers() {
		UserEntity creator = userRepository.save(new UserEntity(CREATOR_ID, "Arif Rahman", "arif@example.com"));
		UserEntity member = userRepository.save(new UserEntity(MEMBER_ID, "Budi Santoso", "budi@example.com"));
		UserEntity otherUser = userRepository.save(new UserEntity(OTHER_USER_ID, "Citra Lestari", "citra@example.com"));

		BillGroupEntity group = billGroupRepository.save(BillGroupEntity.create("Trip Bandung", creator));
		billGroupMemberRepository.saveAll(List.of(
				BillGroupMemberEntity.create(group, creator),
				BillGroupMemberEntity.create(group, member)));
		BillGroupEntity otherGroup = billGroupRepository.save(BillGroupEntity.create("Office Lunch", otherUser));
		billGroupMemberRepository.save(BillGroupMemberEntity.create(otherGroup, otherUser));

		BillGroupEntity savedGroup = billGroupRepository.findById(group.getId()).orElseThrow();
		List<BillGroupMemberEntity> members = billGroupMemberRepository.findByGroup_Id(group.getId());

		assertThat(savedGroup.getCreatedBy().getId()).isEqualTo(CREATOR_ID);
		assertThat(savedGroup.getCreatedAt()).isNotNull();
		assertThat(members)
				.extracting(memberEntity -> memberEntity.getUser().getId())
				.containsExactlyInAnyOrder(CREATOR_ID, MEMBER_ID);
		assertThat(billGroupRepository.findActiveGroupsByMemberUserId(MEMBER_ID))
				.extracting(BillGroupEntity::getId)
				.containsExactly(group.getId());
		assertThat(billGroupMemberRepository.existsByGroup_IdAndUser_IdAndDeletedAtIsNull(group.getId(), MEMBER_ID))
				.isTrue();
		assertThat(billGroupMemberRepository.existsByGroup_IdAndUser_IdAndDeletedAtIsNull(group.getId(), OTHER_USER_ID))
				.isFalse();
		assertThat(billGroupMemberRepository.findActiveMembersByGroupId(group.getId()))
				.extracting(memberEntity -> memberEntity.getUser().getId())
				.containsExactly(CREATOR_ID, MEMBER_ID);
		assertThat(billGroupMemberRepository.countByGroup_IdAndDeletedAtIsNull(group.getId())).isEqualTo(2);
	}
}
