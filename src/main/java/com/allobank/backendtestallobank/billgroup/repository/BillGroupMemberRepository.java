package com.allobank.backendtestallobank.billgroup.repository;

import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.billgroup.entity.BillGroupMemberEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillGroupMemberRepository extends JpaRepository<BillGroupMemberEntity, UUID> {

	List<BillGroupMemberEntity> findByGroup_Id(UUID groupId);

	long countByGroup_IdAndDeletedAtIsNull(UUID groupId);
}
