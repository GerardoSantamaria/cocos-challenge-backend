package com.cocos.challenge.infrastructure.persistence.repository;

import java.util.Optional;

import com.cocos.challenge.infrastructure.persistence.entity.UserEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select account from UserEntity account where account.id = :id")
	Optional<UserEntity> lockById(@Param("id") Integer id);

}
