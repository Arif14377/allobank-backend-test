package com.allobank.backendtestallobank.billgroup.repository;

import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.billgroup.entity.BillGroupMemberEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillGroupMemberRepository extends JpaRepository<BillGroupMemberEntity, UUID> {

	List<BillGroupMemberEntity> findByGroup_Id(UUID groupId);

	@Query("""
			select m
			from BillGroupMemberEntity m
			join fetch m.user
			where m.group.id = :groupId
				and m.deletedAt is null
			order by m.joinedAt asc
			""")
	List<BillGroupMemberEntity> findActiveMembersByGroupId(@Param("groupId") UUID groupId);

	boolean existsByGroup_IdAndUser_IdAndDeletedAtIsNull(UUID groupId, UUID userId);

	long countByGroup_IdAndDeletedAtIsNull(UUID groupId);
}
