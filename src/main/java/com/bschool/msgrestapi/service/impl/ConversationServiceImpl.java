package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.ConversationService;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Autowired
    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Conversation> listForUser(Long userId){
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
        throw new BusinessException("À implémenter — US6 (Charbel)");
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
