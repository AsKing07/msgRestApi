package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.Friendship;
import com.bschool.msgrestapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByUserLowAndUserHigh(User userLow, User userHigh);

    @Query("""
            SELECT f FROM Friendship f
            WHERE f.userLow = :user OR f.userHigh = :user
            """)
    List<Friendship> findAllByUser(@Param("user") User user);
}
