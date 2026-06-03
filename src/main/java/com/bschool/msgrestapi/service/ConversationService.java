package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.Message;

import java.util.List;

public interface ConversationService {

    List<Conversation> listForUser(Long userId);

    Conversation getOrCreateBetweenFriends(Long userId, Long friendId);

    List<Message> listMessages(Long conversationId, Long userId);

    Message sendMessage(Long conversationId, Long senderId, String content);

    Message editMessage(Long messageId, Long userId, String content);

    Message deleteMessage(Long messageId, Long userId);
}
