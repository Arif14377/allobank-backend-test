package com.allobank.backendtestallobank.repository.billgroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allobank.backendtestallobank.entity.billgroup.BillGroupEntity;

public interface BillGroupRepository extends JpaRepository<BillGroupEntity, UUID> {

	@Query("""
			select g
			from BillGroupEntity g
			join BillGroupMemberEntity m on m.group = g
			where m.user.id = :userId
				and m.deletedAt is null
				and g.deletedAt is null
			order by g.createdAt desc
			""")
	List<BillGroupEntity> findActiveGroupsByMemberUserId(@Param("userId") UUID userId);
	Optional<BillGroupEntity> findByIdAndDeletedAtIsNull(UUID id);
}
