package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByParticipantLowAndParticipantHigh(User participantLow, User participantHigh);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.participantLow = :user OR c.participantHigh = :user
            ORDER BY COALESCE(c.lastActivityAt, c.createdAt) DESC
            """)
    List<Conversation> findAllByParticipantOrderByLastActivity(@Param("user") User user);
}
