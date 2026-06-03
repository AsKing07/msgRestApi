package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.Attachment;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.dto.response.AttachmentDownload;
import com.bschool.msgrestapi.dto.response.AttachmentResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.AttachmentRepository;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.AttachmentService;
import com.bschool.msgrestapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${app.file.storage-dir:uploads/files}")
    private String storageDirectory;

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
        String storageKey = conversationId + "/" + UUID.randomUUID();
        Path root = storageRoot();
        Path target = root.resolve(storageKey).normalize();

        if (!target.startsWith(root)) {
            throw new BusinessException("Chemin de stockage invalide.");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target);
        } catch (IOException ex) {
            throw new BusinessException("Impossible d'enregistrer le fichier.");
        }

        Attachment attachment = Attachment.builder()
                .conversation(conversation)
                .uploader(uploader)
                .originalFileName(originalFileName)
                .storageKey(storageKey)
                .sizeBytes(file.getSize())
                .contentType(contentType(file))
                .uploadedAt(now)
                .deleted(false)
                .build();

        conversation.setLastActivityAt(now);
        conversationRepository.save(conversation);

        Attachment saved = attachmentRepository.save(attachment);
        notificationService.notifyNewFile(saved);
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

        Path root = storageRoot();
        Path target = root.resolve(attachment.getStorageKey()).normalize();

        if (!target.startsWith(root)) {
            throw new BusinessException("Chemin de stockage invalide.");
        }

        Resource resource;
        try {
            resource = new UrlResource(target.toUri());
        } catch (MalformedURLException ex) {
            throw new BusinessException("Chemin de fichier invalide.");
        }

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Fichier physique introuvable.");
        }

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

        removePhysicalFile(attachment);
        attachment.setDeleted(true);
        attachment.setDeletedAt(Instant.now());
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

    private Path storageRoot() {
        try {
            Path root = Paths.get(storageDirectory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            return root;
        } catch (IOException ex) {
            throw new BusinessException("Impossible de préparer le dossier de stockage des fichiers.");
        }
    }

    private void removePhysicalFile(Attachment attachment) {
        Path root = storageRoot();
        Path target = root.resolve(attachment.getStorageKey()).normalize();

        if (!target.startsWith(root)) {
            throw new BusinessException("Chemin de stockage invalide.");
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new BusinessException("Impossible de supprimer le fichier physique.");
        }
    }
}
