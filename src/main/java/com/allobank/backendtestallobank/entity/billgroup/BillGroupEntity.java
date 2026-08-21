package com.allobank.backendtestallobank.entity.billgroup;

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
@Table(name = "bill_groups")
public class BillGroupEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private UserEntity createdBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected BillGroupEntity() {
	}

	private BillGroupEntity(UUID id, String name, UserEntity createdBy) {
		this.id = id;
		this.name = name;
		this.createdBy = createdBy;
	}

	public static BillGroupEntity create(String name, UserEntity createdBy) {
		return new BillGroupEntity(UUID.randomUUID(), name, createdBy);
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

	public String getName() {
		return name;
	}

	public UserEntity getCreatedBy() {
		return createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
