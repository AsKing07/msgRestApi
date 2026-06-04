package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    @Query("""
            SELECT DISTINCT groupMember.group FROM ChatGroupMember groupMember
            WHERE groupMember.user = :user
            ORDER BY groupMember.group.createdAt DESC
            """)
    List<ChatGroup> findAllByMemberOrderByCreatedAtDesc(@Param("user") User user);
}
