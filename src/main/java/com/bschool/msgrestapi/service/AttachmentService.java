package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.domain.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    Attachment upload(Long conversationId, Long uploaderId, MultipartFile file);

    void delete(Long attachmentId, Long userId);

    List<Attachment> listByConversation(Long conversationId, Long userId);
}
