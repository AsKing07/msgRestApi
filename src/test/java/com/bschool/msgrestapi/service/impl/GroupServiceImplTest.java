package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.ChatGroupMember;
import com.bschool.msgrestapi.domain.entity.Friendship;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.dto.response.GroupResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.ChatGroupMemberRepository;
import com.bschool.msgrestapi.repository.ChatGroupRepository;
import com.bschool.msgrestapi.repository.FriendshipRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private ChatGroupRepository chatGroupRepository;

    @Mock
    private ChatGroupMemberRepository chatGroupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    private GroupServiceImpl groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupServiceImpl(
                chatGroupRepository,
                chatGroupMemberRepository,
                userRepository,
                friendshipRepository
        );
    }

    @Test
    void createGroupAddsOwnerAndFriendMembers() {
        User owner = user(1L);
        User friend = user(2L);
        ChatGroup group = group(10L, owner);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));
        when(friendshipRepository.findByUserLowAndUserHigh(owner, friend))
                .thenReturn(Optional.of(Friendship.builder().build()));
        when(chatGroupRepository.save(any(ChatGroup.class))).thenReturn(group);
        when(chatGroupMemberRepository.save(any(ChatGroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatGroupMemberRepository.findAllByGroupOrderByJoinedAtAsc(group))
                .thenReturn(List.of(member(group, owner), member(group, friend)));

        GroupResponse response = groupService.createGroup(1L, "Projet", List.of(2L));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getMemberCount()).isEqualTo(2);
        assertThat(response.getMembers()).extracting("userId").containsExactly(1L, 2L);
    }

    @Test
    void createGroupRejectsMoreThanFivePeopleIncludingOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> groupService.createGroup(1L, "Projet", List.of(2L, 3L, 4L, 5L, 6L)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un groupe ne peut pas dépasser 5 personnes.");

        verify(chatGroupRepository, never()).save(any(ChatGroup.class));
    }

    @Test
    void addMemberRequiresOwner() {
        User owner = user(1L);
        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group(10L, owner)));

        assertThatThrownBy(() -> groupService.addMember(10L, 2L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Seul le propriétaire du groupe peut effectuer cette action.");

        verify(chatGroupMemberRepository, never()).save(any(ChatGroupMember.class));
    }

    @Test
    void addMemberRequiresFriendshipWithOwner() {
        User owner = user(1L);
        User member = user(2L);
        ChatGroup group = group(10L, owner);

        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(chatGroupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(false);
        when(chatGroupMemberRepository.countByGroup(group)).thenReturn(1L);
        when(friendshipRepository.findByUserLowAndUserHigh(owner, member)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(10L, 1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le propriétaire peut uniquement ajouter ses amis au groupe.");

        verify(chatGroupMemberRepository, never()).save(any(ChatGroupMember.class));
    }

    @Test
    void removeMemberRejectsOwnerRemoval() {
        User owner = user(1L);
        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group(10L, owner)));

        assertThatThrownBy(() -> groupService.removeMember(10L, 1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le propriétaire ne peut pas être supprimé de son groupe.");
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .firstName("First" + id)
                .lastName("Last" + id)
                .email("user" + id + "@test.local")
                .passwordHash("hash")
                .build();
    }

    private ChatGroup group(Long id, User owner) {
        return ChatGroup.builder()
                .id(id)
                .name("Projet")
                .owner(owner)
                .createdAt(Instant.parse("2026-06-04T10:00:00Z"))
                .build();
    }

    private ChatGroupMember member(ChatGroup group, User user) {
        return ChatGroupMember.builder()
                .group(group)
                .user(user)
                .joinedAt(Instant.parse("2026-06-04T10:00:00Z"))
                .build();
    }
}
