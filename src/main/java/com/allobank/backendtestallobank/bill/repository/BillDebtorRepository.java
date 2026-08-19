package com.allobank.backendtestallobank.bill.repository;

import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.bill.entity.BillDebtorEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillDebtorRepository extends JpaRepository<BillDebtorEntity, UUID> {

	List<BillDebtorEntity> findByBill_Id(UUID billId);

	@Query("""
			select d
			from BillDebtorEntity d
			join fetch d.debtor
			join fetch d.bill b
			where b.group.id = :groupId
			""")
	List<BillDebtorEntity> findDebtorsByGroupId(@Param("groupId") UUID groupId);
}
