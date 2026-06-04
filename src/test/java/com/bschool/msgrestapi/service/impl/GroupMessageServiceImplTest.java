package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.config.AppProperties;
import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.GroupMessage;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.dto.response.GroupMessageResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.ChatGroupMemberRepository;
import com.bschool.msgrestapi.repository.ChatGroupRepository;
import com.bschool.msgrestapi.repository.GroupMessageRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMessageServiceImplTest {

    @Mock
    private ChatGroupRepository chatGroupRepository;

    @Mock
    private ChatGroupMemberRepository chatGroupMemberRepository;

    @Mock
    private GroupMessageRepository groupMessageRepository;

    @Mock
    private UserRepository userRepository;

    private GroupMessageServiceImpl groupMessageService;

    @BeforeEach
    void setUp() {
        groupMessageService = new GroupMessageServiceImpl(
                chatGroupRepository,
                chatGroupMemberRepository,
                groupMessageRepository,
                userRepository,
                new AppProperties(500)
        );
    }

    @Test
    void sendMessageAllowsGroupMember() {
        User owner = user(1L);
        User member = user(2L);
        ChatGroup group = group(10L, owner);

        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(chatGroupMemberRepository.existsByGroupAndUser(group, member)).thenReturn(true);
        when(groupMessageRepository.save(any(GroupMessage.class))).thenAnswer(invocation -> {
            GroupMessage message = invocation.getArgument(0);
            message.setId(100L);
            return message;
        });

        GroupMessageResponse response = groupMessageService.sendMessage(10L, 2L, " Bonjour ");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getGroupId()).isEqualTo(10L);
        assertThat(response.getSenderId()).isEqualTo(2L);
        assertThat(response.getContent()).isEqualTo("Bonjour");
        assertThat(group.getLastActivityAt()).isNotNull();
    }

    @Test
    void sendMessageRejectsNonMember() {
        User owner = user(1L);
        User stranger = user(3L);
        ChatGroup group = group(10L, owner);

        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(3L)).thenReturn(Optional.of(stranger));
        when(chatGroupMemberRepository.existsByGroupAndUser(group, stranger)).thenReturn(false);

        assertThatThrownBy(() -> groupMessageService.sendMessage(10L, 3L, "Bonjour"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vous ne faites pas partie de ce groupe.");

        verify(groupMessageRepository, never()).save(any(GroupMessage.class));
    }

    @Test
    void editMessageAllowsSenderAndKeepsOldContent() {
        User owner = user(1L);
        User member = user(2L);
        ChatGroup group = group(10L, owner);
        GroupMessage message = message(100L, group, member, "Ancien message");

        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMessageRepository.findByIdAndGroup(100L, group)).thenReturn(Optional.of(message));
        when(groupMessageRepository.save(message)).thenReturn(message);

        GroupMessageResponse response = groupMessageService.editMessage(10L, 100L, 2L, "Nouveau message");

        assertThat(response.getContent()).isEqualTo("Nouveau message");
        assertThat(response.getOldContent()).isEqualTo("Ancien message");
        assertThat(response.isEdited()).isTrue();
        assertThat(group.getLastActivityAt()).isNotNull();
    }

    @Test
    void editMessageRejectsNonSender() {
        User owner = user(1L);
        User member = user(2L);
        ChatGroup group = group(10L, owner);
        GroupMessage message = message(100L, group, member, "Ancien message");

        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMessageRepository.findByIdAndGroup(100L, group)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> groupMessageService.editMessage(10L, 100L, 1L, "Nouveau message"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Seul l'auteur du message peut effectuer cette action.");

        verify(groupMessageRepository, never()).save(any(GroupMessage.class));
    }

    @Test
    void deleteMessageAllowsSender() {
        User owner = user(1L);
        User member = user(2L);
        ChatGroup group = group(10L, owner);
        GroupMessage message = message(100L, group, member, "Message");

        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMessageRepository.findByIdAndGroup(100L, group)).thenReturn(Optional.of(message));
        when(groupMessageRepository.save(message)).thenReturn(message);

        GroupMessageResponse response = groupMessageService.deleteMessage(10L, 100L, 2L);

        assertThat(response.isDeleted()).isTrue();
        assertThat(response.getDeletedAt()).isNotNull();
        assertThat(group.getLastActivityAt()).isNotNull();
    }

    @Test
    void deleteMessageRejectsNonSender() {
        User owner = user(1L);
        User member = user(2L);
        ChatGroup group = group(10L, owner);
        GroupMessage message = message(100L, group, member, "Message");

        when(chatGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMessageRepository.findByIdAndGroup(100L, group)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> groupMessageService.deleteMessage(10L, 100L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Seul l'auteur du message peut effectuer cette action.");

        verify(groupMessageRepository, never()).save(any(GroupMessage.class));
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
                .name("Groupe")
                .owner(owner)
                .createdAt(Instant.parse("2026-06-04T10:00:00Z"))
                .build();
    }

    private GroupMessage message(Long id, ChatGroup group, User sender, String content) {
        return GroupMessage.builder()
                .id(id)
                .group(group)
                .sender(sender)
                .content(content)
                .createdAt(Instant.parse("2026-06-04T10:00:00Z"))
                .edited(false)
                .deleted(false)
                .build();
    }
}
