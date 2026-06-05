package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.Attachment;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.dto.response.AttachmentDownload;
import com.bschool.msgrestapi.dto.response.AttachmentResponse;
import com.bschool.msgrestapi.repository.AttachmentRepository;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.AuditService;
import com.bschool.msgrestapi.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    private AttachmentServiceImpl attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentServiceImpl(
                attachmentRepository,
                conversationRepository,
                userRepository,
                notificationService,
                auditService
        );
    }

    @Test
    void uploadStoresFileContentInDatabaseBlob() {
        User uploader = user(1L);
        User friend = user(2L);
        Conversation conversation = conversation(10L, uploader, friend);
        byte[] content = "Bonjour fichier".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", content);

        when(userRepository.findById(1L)).thenReturn(Optional.of(uploader));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
            Attachment attachment = invocation.getArgument(0);
            attachment.setId(100L);
            return attachment;
        });

        AttachmentResponse response = attachmentService.upload(10L, 1L, file);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.originalFileName()).isEqualTo("note.txt");
        assertThat(response.sizeBytes()).isEqualTo((long) content.length);
        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getData()).isEqualTo(content);
        assertThat(attachmentCaptor.getValue().getStorageKey()).isNotBlank();
        verify(notificationService).notifyNewFile(any(Attachment.class));
    }

    @Test
    void uploadAllowsNonImageFiles() {
        User uploader = user(1L);
        User friend = user(2L);
        Conversation conversation = conversation(10L, uploader, friend);
        byte[] content = new byte[]{0x50, 0x4b, 0x03, 0x04, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "archive.zip", "application/zip", content);

        when(userRepository.findById(1L)).thenReturn(Optional.of(uploader));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
            Attachment attachment = invocation.getArgument(0);
            attachment.setId(101L);
            return attachment;
        });

        AttachmentResponse response = attachmentService.upload(10L, 1L, file);

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.originalFileName()).isEqualTo("archive.zip");
        assertThat(response.contentType()).isEqualTo("application/zip");
        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getData()).isEqualTo(content);
    }

    @Test
    void downloadReadsFileContentFromDatabaseBlob() throws Exception {
        User uploader = user(1L);
        User friend = user(2L);
        Conversation conversation = conversation(10L, uploader, friend);
        byte[] content = "Contenu en base".getBytes(StandardCharsets.UTF_8);
        Attachment attachment = attachment(100L, conversation, uploader, content);

        when(attachmentRepository.findByIdAndConversation_Id(100L, 10L)).thenReturn(Optional.of(attachment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(uploader));

        AttachmentDownload download = attachmentService.download(10L, 100L, 1L);

        assertThat(download.fileName()).isEqualTo("note.txt");
        assertThat(download.contentType()).isEqualTo("text/plain");
        assertThat(download.sizeBytes()).isEqualTo((long) content.length);
        assertThat(download.resource().getInputStream().readAllBytes()).isEqualTo(content);
    }

    @Test
    void deleteMarksAttachmentDeletedAndClearsDatabaseBlob() {
        User uploader = user(1L);
        User friend = user(2L);
        Conversation conversation = conversation(10L, uploader, friend);
        Attachment attachment = attachment(100L, conversation, uploader, "data".getBytes(StandardCharsets.UTF_8));

        when(attachmentRepository.findByIdAndConversation_Id(100L, 10L)).thenReturn(Optional.of(attachment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(uploader));
        when(attachmentRepository.save(attachment)).thenReturn(attachment);

        attachmentService.delete(10L, 100L, 1L);

        assertThat(attachment.isDeleted()).isTrue();
        assertThat(attachment.getDeletedAt()).isNotNull();
        assertThat(attachment.getData()).isNull();
        verify(notificationService).notifyFileDeleted(attachment);
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .firstName("First" + id)
                .lastName("Last" + id)
                .email("user" + id + "@test.local")
                .passwordHash("hash")
                .build();
    }

    private Conversation conversation(Long id, User participantLow, User participantHigh) {
        return Conversation.builder()
                .id(id)
                .participantLow(participantLow)
                .participantHigh(participantHigh)
                .createdAt(Instant.parse("2026-06-04T10:00:00Z"))
                .lastActivityAt(Instant.parse("2026-06-04T10:00:00Z"))
                .build();
    }

    private Attachment attachment(Long id, Conversation conversation, User uploader, byte[] data) {
        return Attachment.builder()
                .id(id)
                .conversation(conversation)
                .uploader(uploader)
                .originalFileName("note.txt")
                .storageKey("opaque-key")
                .data(data)
                .sizeBytes((long) data.length)
                .contentType("text/plain")
                .uploadedAt(Instant.parse("2026-06-04T10:00:00Z"))
                .deleted(false)
                .build();
    }
}
