package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    List<GroupMessage> findByGroupAndDeletedFalseOrderByCreatedAtAsc(ChatGroup group);

    Optional<GroupMessage> findByIdAndGroup(Long id, ChatGroup group);
}
