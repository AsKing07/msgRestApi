package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.config.AppProperties;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.Message;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.util.UserPairUtil;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.FriendshipRepository;
import com.bschool.msgrestapi.repository.MessageRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.ConversationService;
import com.bschool.msgrestapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final AppProperties appProperties;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> listForUser(Long userId) {
        User user = requireUser(userId);
        return conversationRepository.findAllByParticipantOrderByLastActivity(user);
    }

    @Override
    @Transactional
    public Conversation getOrCreateBetweenFriends(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new BusinessException("Impossible de créer une discussion avec soi-même.");
        }

        User user = requireUser(userId);
        User friend = requireUser(friendId);
        var orderedUsers = UserPairUtil.order(user, friend);

        if (friendshipRepository.findByUserLowAndUserHigh(orderedUsers.low(), orderedUsers.high()).isEmpty()) {
            throw new BusinessException("Une amitié doit exister avant de créer une discussion.");
        }

        Instant now = Instant.now();
        return conversationRepository
                .findByParticipantLowAndParticipantHigh(orderedUsers.low(), orderedUsers.high())
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .participantLow(orderedUsers.low())
                        .participantHigh(orderedUsers.high())
                        .createdAt(now)
                        .lastActivityAt(now)
                        .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> listMessages(Long conversationId, Long userId) {
        throw new BusinessException("À implémenter — Charbel");
    }

    @Override
    @Transactional
    public Message sendMessage(Long conversationId, Long senderId, String content) {
        validateMessageContent(content);

        Conversation conversation = requireConversation(conversationId);
        assertParticipant(conversation, senderId);

        User sender = requireUser(senderId);
        Instant now = Instant.now();

        conversation.setLastActivityAt(now);
        conversationRepository.save(conversation);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .edited(false)
                .deleted(false)
                .build();

        Message saved = messageRepository.save(message);
        notificationService.notifyNewMessage(saved);
        return saved;
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

    private Conversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable."));
    }

    private void assertParticipant(Conversation conversation, Long userId) {
        boolean isParticipant = conversation.getParticipantLow().getId().equals(userId)
                || conversation.getParticipantHigh().getId().equals(userId);

        if (!isParticipant) {
            throw new BusinessException("Vous ne participez pas à cette discussion.");
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    @Override
    public Message editMessage(Long messageId, Long userId, String content) {
        throw new BusinessException("À implémenter — US10 (Ivan)");
    }

    @Override
    public void deleteMessage(Long messageId, Long userId) {
        throw new BusinessException("À implémenter — US13 (Ivan)");
    }
}
