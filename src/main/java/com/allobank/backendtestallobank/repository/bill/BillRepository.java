package com.allobank.backendtestallobank.repository.bill;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allobank.backendtestallobank.entity.bill.BillEntity;

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
