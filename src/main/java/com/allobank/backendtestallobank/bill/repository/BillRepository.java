package com.allobank.backendtestallobank.bill.repository;

import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.bill.entity.BillEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<BillEntity, UUID> {

	@Query("""
			select b
			from BillEntity b
			join fetch b.payer
			where b.group.id = :groupId
			order by b.createdAt desc
			""")
	List<BillEntity> findBillsByGroupId(@Param("groupId") UUID groupId);
}
