package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.ChatGroupMember;
import com.bschool.msgrestapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {

    boolean existsByGroupAndUser(ChatGroup group, User user);

    long countByGroup(ChatGroup group);

    Optional<ChatGroupMember> findByGroupAndUser(ChatGroup group, User user);

    List<ChatGroupMember> findAllByGroupOrderByJoinedAtAsc(ChatGroup group);
}
