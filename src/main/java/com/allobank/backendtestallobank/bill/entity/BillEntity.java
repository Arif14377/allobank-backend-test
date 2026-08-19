package com.allobank.backendtestallobank.bill.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.allobank.backendtestallobank.billgroup.entity.BillGroupEntity;
import com.allobank.backendtestallobank.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "bills")
public class BillEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private BillGroupEntity group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payer_id", nullable = false)
	private UserEntity payer;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column
	private String description;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected BillEntity() {
	}

	private BillEntity(UUID id, BillGroupEntity group, UserEntity payer, BigDecimal amount, String description) {
		this.id = id;
		this.group = group;
		this.payer = payer;
		this.amount = amount;
		this.description = description;
	}

	public static BillEntity create(BillGroupEntity group, UserEntity payer, BigDecimal amount, String description) {
		String normalizedDescription = description == null ? null : description.trim();
		return new BillEntity(UUID.randomUUID(), group, payer, amount, normalizedDescription);
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public BillGroupEntity getGroup() {
		return group;
	}

	public UserEntity getPayer() {
		return payer;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getDescription() {
		return description;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
