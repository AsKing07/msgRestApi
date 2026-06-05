package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.config.AppProperties;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.Message;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.AuditAction;
import com.bschool.msgrestapi.domain.enums.AuditResourceType;
import com.bschool.msgrestapi.domain.util.UserPairUtil;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.FriendshipRepository;
import com.bschool.msgrestapi.repository.MessageRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.AuditService;
import com.bschool.msgrestapi.service.ConversationService;
import com.bschool.msgrestapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final AppProperties appProperties;
    private final NotificationService notificationService;
    private final AuditService auditService;

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
        Conversation conversation = requireConversation(conversationId);
        assertParticipant(conversation, userId);
        return messageRepository.findByConversationAndDeletedFalseOrderByCreatedAtAsc(conversation);
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
        auditService.log(
                senderId,
                AuditResourceType.MESSAGE,
                AuditAction.SENT,
                saved.getId(),
                saved.getContent()
        );
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
    @Transactional
    public Message editMessage(Long messageId, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("Le contenu du message ne peut pas être vide.");
        } else if (content.length() > 500) {
            throw new BusinessException("Le contenu du message ne peut pas dépasser 500 caractères.");
        } else {
            Optional<Message> message = messageRepository.findById(messageId);

            if (message.get().getContent().equals(content)){
                throw new BusinessException("Le contenu du message est le meme. Veuiller le modifier !");
            } else {
                message.get().setOldContent(message.get().getContent());
                message.get().setContent(content);
                message.get().setUpdatedAt(java.time.Instant.now());
                message.get().setEdited(true);


                Optional<Conversation> conv = conversationRepository.findById(message.get().getConversation().getId());
                conv.get().setLastActivityAt(java.time.Instant.now());

                //MISE A JOUR DE LA CONVERSATION AVEC LA DERNIERE ACTIVITE
                conversationRepository.save(conv.get());

                //ENREGISTREMENT DU MESSAGE
                Message saved = messageRepository.save(message.get());
                notificationService.notifyMessageEdited(saved);
                auditService.log(
                        userId,
                        AuditResourceType.MESSAGE,
                        AuditAction.EDITED,
                        saved.getId(),
                        saved.getContent()
                );
                return saved;
            }
        }
    }

    @Override
    @Transactional
    public Message deleteMessage(Long messageId, Long userId) {
        Optional<Message> message = messageRepository.findById(messageId);
        if (message.isEmpty()) {
            throw new BusinessException("Message non trouvé : " + messageId);
        }
        Message sms = message.get();
        assertParticipant(sms.getConversation(), userId);
        if (sms.isDeleted()) {
            throw new BusinessException("Ce message a déjà été supprimé.");
        }
        Long idSender = sms.getSender().getId();
        if (!idSender.equals(userId)) {
            throw new BusinessException("VOUS N'AVEZ PAS DE DROIT DE SUPPRESSION");
        }
        sms.setDeletedAt(Instant.now());
        sms.setDeleted(true);
        Message saved = messageRepository.save(sms);
        notificationService.notifyMessageDeleted(saved);
        auditService.log(
                userId,
                AuditResourceType.MESSAGE,
                AuditAction.DELETED,
                saved.getId(),
                saved.getContent()
        );
        return saved;
    }
}
