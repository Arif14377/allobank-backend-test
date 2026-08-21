package com.allobank.backendtestallobank.repository.bill;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allobank.backendtestallobank.entity.bill.BillDebtorEntity;

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
