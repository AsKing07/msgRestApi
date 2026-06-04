package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    @Query("""
            SELECT DISTINCT g FROM ChatGroupMember groupMember
            JOIN groupMember.group g
            WHERE groupMember.user = :user
            ORDER BY COALESCE(g.lastActivityAt, g.createdAt) DESC
            """)
    List<ChatGroup> findAllByMemberOrderByLastActivityDesc(@Param("user") User user);
}
