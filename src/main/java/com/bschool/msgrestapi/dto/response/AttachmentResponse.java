package com.bschool.msgrestapi.dto.response;

import com.bschool.msgrestapi.domain.entity.Attachment;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        Long conversationId,
        Long uploaderId,
        String originalFileName,
        Long sizeBytes,
        String contentType,
        Instant uploadedAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getConversation().getId(),
                attachment.getUploader().getId(),
                attachment.getOriginalFileName(),
                attachment.getSizeBytes(),
                attachment.getContentType(),
                attachment.getUploadedAt()
        );
    }
}
