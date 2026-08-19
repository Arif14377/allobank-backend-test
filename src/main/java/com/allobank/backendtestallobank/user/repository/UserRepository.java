package com.allobank.backendtestallobank.user.repository;

import java.util.UUID;

import com.allobank.backendtestallobank.user.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
}
