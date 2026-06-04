package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.dto.response.GroupMessageResponse;

import java.util.List;

public interface GroupMessageService {

    List<GroupMessageResponse> listMessages(Long groupId, Long userId);

    GroupMessageResponse sendMessage(Long groupId, Long senderId, String content);

    GroupMessageResponse editMessage(Long groupId, Long messageId, Long userId, String content);

    GroupMessageResponse deleteMessage(Long groupId, Long messageId, Long userId);
}
