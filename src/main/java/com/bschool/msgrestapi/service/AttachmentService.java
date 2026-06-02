package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.dto.response.AttachmentDownload;
import com.bschool.msgrestapi.dto.response.AttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    AttachmentResponse upload(Long conversationId, Long uploaderId, MultipartFile file);

    List<AttachmentResponse> listByConversation(Long conversationId, Long userId);

    AttachmentDownload download(Long conversationId, Long attachmentId, Long userId);

    void delete(Long conversationId, Long attachmentId, Long userId);
}
