package com.allobank.backendtestallobank.bill.repository;

import java.util.List;
import java.util.UUID;

import com.allobank.backendtestallobank.bill.entity.BillDebtorEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillDebtorRepository extends JpaRepository<BillDebtorEntity, UUID> {

	List<BillDebtorEntity> findByBill_Id(UUID billId);
}
