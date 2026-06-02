package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationAndDeletedFalseOrderByCreatedAtAsc(Conversation conversation);
}
