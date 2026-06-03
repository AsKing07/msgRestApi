package com.bschool.msgrestapi.service.impl;

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
    @Override
    public List<Conversation> listForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé" + userId));

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

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    @Override
    public List<Message> listMessages(Long conversationId, Long userId) {        throw new BusinessException("À implémenter — Charbel");
    }

    @Override
    public Message sendMessage(Long conversationId, Long senderId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("Le contenu du message ne peut pas être vide.");
        } else if (content.length() > 500) {
            throw new BusinessException("Le contenu du message ne peut pas dépasser 500 caractères.");
        } else {
            Optional<Conversation> conversation = conversationRepository.findById(conversationId);

            if(conversation.isEmpty()) {
                throw new BusinessException("Conversation non trouvée : " + conversationId);
            }else{
                Conversation conv = conversation.get();
                if (!conv.getParticipantLow().getId().equals(senderId) && !conv.getParticipantHigh().getId().equals(senderId)) {
                    throw new BusinessException("L'utilisateur n'est pas participant de la conversation : " + conversationId);
                }else {
                    Message message = new Message();
                    message.setConversation(conv);
                    Optional<User> sender = userRepository.findById(senderId);
                    message.setSender(sender.get());
                    message.setContent(content);
                    message.setCreatedAt(java.time.Instant.now());
                    message.setEdited(false);
                    message.setDeleted(false);
                    conv.setLastActivityAt(java.time.Instant.now());

                    //MISE A JOUR DE LA CONVERSATION AVEC LA DERNIERE ACTIVITE
                    conversationRepository.save(conv);

                    //ENREGISTREMENT DU MESSAGE
                    return messageRepository.save(message);
                }
            }
        }
    }

    @Override
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
                return messageRepository.save(message.get());
            }
        }
    }

    @Override
    public void deleteMessage(Long messageId, Long userId) {
        throw new BusinessException("À implémenter — US13 (Ivan)");
    }
}
