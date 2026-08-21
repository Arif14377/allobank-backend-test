package com.allobank.backendtestallobank.entity.bill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.allobank.backendtestallobank.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "bill_debtors")
public class BillDebtorEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bill_id", nullable = false)
	private BillEntity bill;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "debtor_id", nullable = false)
	private UserEntity debtor;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BillDebtorEntity() {
	}

	private BillDebtorEntity(UUID id, BillEntity bill, UserEntity debtor, BigDecimal amount) {
		this.id = id;
		this.bill = bill;
		this.debtor = debtor;
		this.amount = amount;
	}

	public static BillDebtorEntity create(BillEntity bill, UserEntity debtor, BigDecimal amount) {
		return new BillDebtorEntity(UUID.randomUUID(), bill, debtor, amount);
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public BillEntity getBill() {
		return bill;
	}

	public UserEntity getDebtor() {
		return debtor;
	}

	public BigDecimal getAmount() {
		return amount;
	}
}
