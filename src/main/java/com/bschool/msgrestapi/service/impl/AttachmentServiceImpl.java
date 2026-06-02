package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.service.AttachmentService;
import com.bschool.msgrestapi.domain.entity.Attachment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    @Override
    public Attachment upload(Long conversationId, Long uploaderId, MultipartFile file) {
        throw new BusinessException("À implémenter — US8 (Ivan)");
    }

    @Override
    public void delete(Long attachmentId, Long userId) {
        throw new BusinessException("À implémenter — US9 (Ivan)");
    }

    @Override
    public List<Attachment> listByConversation(Long conversationId, Long userId) {
        throw new BusinessException("À implémenter — Ivan");
    }
}
