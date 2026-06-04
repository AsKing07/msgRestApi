package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.config.AppProperties;
import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.GroupMessage;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.dto.response.GroupMessageResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.ChatGroupMemberRepository;
import com.bschool.msgrestapi.repository.ChatGroupRepository;
import com.bschool.msgrestapi.repository.GroupMessageRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.GroupMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupMessageServiceImpl implements GroupMessageService {

    private final ChatGroupRepository chatGroupRepository;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Override
    @Transactional(readOnly = true)
    public List<GroupMessageResponse> listMessages(Long groupId, Long userId) {
        ChatGroup group = requireGroup(groupId);
        User user = requireUser(userId);
        assertMember(group, user);

        return groupMessageRepository.findByGroupAndDeletedFalseOrderByCreatedAtAsc(group)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GroupMessageResponse sendMessage(Long groupId, Long senderId, String content) {
        validateMessageContent(content);

        ChatGroup group = requireGroup(groupId);
        User sender = requireUser(senderId);
        assertMember(group, sender);

        Instant now = Instant.now();
        group.setLastActivityAt(now);
        chatGroupRepository.save(group);

        GroupMessage message = groupMessageRepository.save(GroupMessage.builder()
                .group(group)
                .sender(sender)
                .content(content.trim())
                .edited(false)
                .deleted(false)
                .build());

        return toResponse(message);
    }

    @Override
    @Transactional
    public GroupMessageResponse editMessage(Long groupId, Long messageId, Long userId, String content) {
        validateMessageContent(content);

        ChatGroup group = requireGroup(groupId);
        GroupMessage message = requireMessage(group, messageId);
        assertMessageOwner(message, userId);

        if (message.isDeleted()) {
            throw new BusinessException("Ce message de groupe a déjà été supprimé.");
        }
        String normalizedContent = content.trim();
        if (message.getContent().equals(normalizedContent)) {
            throw new BusinessException("Le contenu du message est identique. Veuillez le modifier.");
        }

        Instant now = Instant.now();
        message.setOldContent(message.getContent());
        message.setContent(normalizedContent);
        message.setUpdatedAt(now);
        message.setEdited(true);
        group.setLastActivityAt(now);
        chatGroupRepository.save(group);

        return toResponse(groupMessageRepository.save(message));
    }

    @Override
    @Transactional
    public GroupMessageResponse deleteMessage(Long groupId, Long messageId, Long userId) {
        ChatGroup group = requireGroup(groupId);
        GroupMessage message = requireMessage(group, messageId);
        assertMessageOwner(message, userId);

        if (message.isDeleted()) {
            throw new BusinessException("Ce message de groupe a déjà été supprimé.");
        }

        Instant now = Instant.now();
        message.setDeleted(true);
        message.setDeletedAt(now);
        group.setLastActivityAt(now);
        chatGroupRepository.save(group);

        return toResponse(groupMessageRepository.save(message));
    }

    private void validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("Le contenu du message ne peut pas être vide.");
        }
        if (content.length() > appProperties.maxLength()) {
            throw new BusinessException(
                    "Le contenu du message ne peut pas dépasser %d caractères.".formatted(appProperties.maxLength())
            );
        }
    }

    private void assertMember(ChatGroup group, User user) {
        if (!chatGroupMemberRepository.existsByGroupAndUser(group, user)) {
            throw new BusinessException("Vous ne faites pas partie de ce groupe.");
        }
    }

    private void assertMessageOwner(GroupMessage message, Long userId) {
        if (!message.getSender().getId().equals(userId)) {
            throw new BusinessException("Seul l'auteur du message peut effectuer cette action.");
        }
    }

    private GroupMessage requireMessage(ChatGroup group, Long messageId) {
        return groupMessageRepository.findByIdAndGroup(messageId, group)
                .orElseThrow(() -> new ResourceNotFoundException("Message de groupe introuvable."));
    }

    private ChatGroup requireGroup(Long groupId) {
        return chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable."));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private GroupMessageResponse toResponse(GroupMessage message) {
        User sender = message.getSender();
        return GroupMessageResponse.builder()
                .id(message.getId())
                .groupId(message.getGroup().getId())
                .senderId(sender.getId())
                .senderFirstName(sender.getFirstName())
                .senderLastName(sender.getLastName())
                .content(message.getContent())
                .oldContent(message.getOldContent())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .edited(message.isEdited())
                .deleted(message.isDeleted())
                .deletedAt(message.getDeletedAt())
                .build();
    }
}
