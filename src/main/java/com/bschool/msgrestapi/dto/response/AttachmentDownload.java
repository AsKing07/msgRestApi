package com.bschool.msgrestapi.dto.response;

import org.springframework.core.io.Resource;

public record AttachmentDownload(
        String fileName,
        String contentType,
        Long sizeBytes,
        Resource resource
) {
}
