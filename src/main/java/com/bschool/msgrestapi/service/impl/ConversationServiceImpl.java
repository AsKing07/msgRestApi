package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.MessageRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.ConversationService;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private final MessageRepository messageRepository;

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Autowired
    public ConversationServiceImpl(
            MessageRepository messageRepository, ConversationRepository conversationRepository,
            UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Conversation> listForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé" + userId));

        return conversationRepository.findAllByParticipantOrderByLastActivity(user);
    }

    @Override
    public Conversation getOrCreateBetweenFriends(Long userId, Long friendId) {
        throw new BusinessException("À implémenter — US6 (Charbel)");
    }

    @Override
    public List<Message> listMessages(Long conversationId, Long userId) {
        throw new BusinessException("À implémenter — Charbel");
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
           // throw new BusinessException("À implémenter — US6 (Charbel)");
        }
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
