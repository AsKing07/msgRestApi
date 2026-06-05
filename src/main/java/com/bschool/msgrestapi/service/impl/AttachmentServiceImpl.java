package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.Attachment;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.AuditAction;
import com.bschool.msgrestapi.domain.enums.AuditResourceType;
import com.bschool.msgrestapi.dto.response.AttachmentDownload;
import com.bschool.msgrestapi.dto.response.AttachmentResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.AttachmentRepository;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.AttachmentService;
import com.bschool.msgrestapi.service.AuditService;
import com.bschool.msgrestapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override
    @Transactional
    public AttachmentResponse upload(Long conversationId, Long uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier est obligatoire.");
        }

        User uploader = requireUser(uploaderId);
        Conversation conversation = requireConversation(conversationId);
        assertParticipant(conversation, uploader.getId());

        Instant now = Instant.now();
        String originalFileName = cleanOriginalFileName(file);
        byte[] data = readFileData(file);

        Attachment attachment = Attachment.builder()
                .conversation(conversation)
                .uploader(uploader)
                .originalFileName(originalFileName)
                .storageKey(UUID.randomUUID().toString())
                .data(data)
                .sizeBytes(file.getSize())
                .contentType(contentType(file))
                .uploadedAt(now)
                .deleted(false)
                .build();

        conversation.setLastActivityAt(now);
        conversationRepository.save(conversation);

        Attachment saved = attachmentRepository.save(attachment);
        notificationService.notifyNewFile(saved);
        auditService.log(
                uploaderId,
                AuditResourceType.FILE,
                AuditAction.SENT,
                saved.getId(),
                saved.getOriginalFileName()
        );
        return AttachmentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listByConversation(Long conversationId, Long userId) {
        User user = requireUser(userId);
        Conversation conversation = requireConversation(conversationId);
        assertParticipant(conversation, user.getId());

        return attachmentRepository.findByConversationAndDeletedFalseOrderByUploadedAtAsc(conversation)
                .stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownload download(Long conversationId, Long attachmentId, Long userId) {
        Attachment attachment = requireAvailableAttachment(conversationId, attachmentId);
        User user = requireUser(userId);
        assertParticipant(attachment.getConversation(), user.getId());

        byte[] data = attachment.getData();
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("Contenu du fichier introuvable en base.");
        }
        Resource resource = new ByteArrayResource(data);

        return new AttachmentDownload(
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                resource
        );
    }

    @Override
    @Transactional
    public void delete(Long conversationId, Long attachmentId, Long userId) {
        Attachment attachment = requireAvailableAttachment(conversationId, attachmentId);
        User user = requireUser(userId);
        assertParticipant(attachment.getConversation(), user.getId());
        assertUploader(attachment, user.getId(), "Seul l'expéditeur peut supprimer ce fichier.");

        attachment.setData(null);
        attachment.setDeleted(true);
        attachment.setDeletedAt(Instant.now());
        Attachment saved = attachmentRepository.save(attachment);
        notificationService.notifyFileDeleted(saved);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private Conversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion introuvable."));
    }

    private Attachment requireAvailableAttachment(Long conversationId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndConversation_Id(attachmentId, conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable dans cette discussion."));

        if (attachment.isDeleted()) {
            throw new BusinessException("Ce fichier n'est plus disponible.");
        }

        return attachment;
    }

    private void assertParticipant(Conversation conversation, Long userId) {
        boolean isParticipant = conversation.getParticipantLow().getId().equals(userId)
                || conversation.getParticipantHigh().getId().equals(userId);

        if (!isParticipant) {
            throw new BusinessException("Vous ne participez pas à cette discussion.");
        }
    }

    private void assertUploader(Attachment attachment, Long userId, String message) {
        if (!attachment.getUploader().getId().equals(userId)) {
            throw new BusinessException(message);
        }
    }

    private String cleanOriginalFileName(MultipartFile file) {
        String originalFileName = StringUtils.hasText(file.getOriginalFilename())
                ? StringUtils.cleanPath(file.getOriginalFilename())
                : "fichier";

        originalFileName = originalFileName.replace("\\", "/");
        int lastSeparator = originalFileName.lastIndexOf('/');
        if (lastSeparator >= 0) {
            originalFileName = originalFileName.substring(lastSeparator + 1);
        }

        if (!StringUtils.hasText(originalFileName) || originalFileName.contains("..")) {
            throw new BusinessException("Nom de fichier invalide.");
        }

        return originalFileName;
    }

    private String contentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private byte[] readFileData(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("Impossible de lire le fichier.");
        }
    }
}
