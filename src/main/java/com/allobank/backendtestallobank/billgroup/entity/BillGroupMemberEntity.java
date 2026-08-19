package com.allobank.backendtestallobank.billgroup.entity;

import java.time.Instant;
import java.util.UUID;

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
@Table(name = "bill_group_members")
public class BillGroupMemberEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private BillGroupEntity group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected BillGroupMemberEntity() {
	}

	private BillGroupMemberEntity(UUID id, BillGroupEntity group, UserEntity user) {
		this.id = id;
		this.group = group;
		this.user = user;
	}

	public static BillGroupMemberEntity create(BillGroupEntity group, UserEntity user) {
		return new BillGroupMemberEntity(UUID.randomUUID(), group, user);
	}

	@PrePersist
	void prePersist() {
		if (joinedAt == null) {
			joinedAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public BillGroupEntity getGroup() {
		return group;
	}

	public UserEntity getUser() {
		return user;
	}
}
