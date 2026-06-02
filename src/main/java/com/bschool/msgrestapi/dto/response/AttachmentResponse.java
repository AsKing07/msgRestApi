package com.bschool.msgrestapi.dto.response;

import com.bschool.msgrestapi.domain.entity.Attachment;
import com.bschool.msgrestapi.domain.enums.AttachmentStatus;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        Long conversationId,
        Long uploaderId,
        String originalFileName,
        Long sizeBytes,
        String contentType,
        AttachmentStatus status,
        Instant uploadedAt,
        Instant cancelledAt,
        Instant declinedAt,
        Instant deletedAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getConversation().getId(),
                attachment.getUploader().getId(),
                attachment.getOriginalFileName(),
                attachment.getSizeBytes(),
                attachment.getContentType(),
                attachment.getStatus(),
                attachment.getUploadedAt(),
                attachment.getCancelledAt(),
                attachment.getDeclinedAt(),
                attachment.getDeletedAt()
        );
    }
}
