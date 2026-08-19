package com.allobank.backendtestallobank.bill.repository;

import java.util.UUID;

import com.allobank.backendtestallobank.bill.entity.BillEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<BillEntity, UUID> {
}
