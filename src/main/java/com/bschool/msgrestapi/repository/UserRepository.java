package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.lastActiveAt = :activeAt WHERE u.id = :userId")
    void updateLastActiveAt(@Param("userId") Long userId, @Param("activeAt") Instant activeAt);
}
