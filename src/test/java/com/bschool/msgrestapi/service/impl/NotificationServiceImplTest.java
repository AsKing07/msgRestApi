package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.config.MailProperties;
import com.bschool.msgrestapi.config.PresenceProperties;
import com.bschool.msgrestapi.domain.entity.Attachment;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Message;
import com.bschool.msgrestapi.domain.entity.Notification;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import com.bschool.msgrestapi.domain.enums.NotificationType;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.NotificationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    private final MailProperties mailProperties = new MailProperties("noreply@test.local");
    private final PresenceProperties presenceProperties = new PresenceProperties(5);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                mailSender,
                mailProperties,
                presenceProperties,
                jsonMapper
        );
    }

    @Test
    void notifyUserPersistsNotificationForRecipient() {
        User recipient = user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(50L);
            return notification;
        });

        Notification result = notificationService.notifyUser(2L, NotificationType.NEW_MESSAGE, "{\"content\":\"Salut\"}");

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getRecipient()).isEqualTo(recipient);
        assertThat(result.getType()).isEqualTo(NotificationType.NEW_MESSAGE);
        assertThat(result.getPayload()).isEqualTo("{\"content\":\"Salut\"}");
        assertThat(result.isEmailSent()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void notifyUserThrowsWhenRecipientMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.notifyUser(99L, NotificationType.NEW_MESSAGE, "{}"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utilisateur introuvable");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void sendEmailIfOfflineSendsMailWhenUserInactive() {
        User recipient = user(2L);
        Notification pendingNotification = Notification.builder()
                .id(50L)
                .recipient(recipient)
                .type(NotificationType.FRIEND_REQUEST_RECEIVED)
                .payload("{\"friendRequestId\":5}")
                .createdAt(Instant.parse("2026-06-04T10:00:00Z"))
                .emailSent(false)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(userRepository.getReferenceById(2L)).thenReturn(recipient);
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient))
                .thenReturn(List.of(pendingNotification));
        when(notificationRepository.save(pendingNotification)).thenReturn(pendingNotification);

        notificationService.sendEmailIfOffline(
                2L,
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "{\"friendRequestId\":5}"
        );

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getFrom()).isEqualTo("noreply@test.local");
        assertThat(mailCaptor.getValue().getTo()).containsExactly("user2@test.local");
        assertThat(mailCaptor.getValue().getSubject()).isEqualTo("Nouvelle demande d'ami");
        assertThat(mailCaptor.getValue().getText()).contains("{\"friendRequestId\":5}");
        assertThat(pendingNotification.isEmailSent()).isTrue();
    }

    @Test
    void sendEmailIfOfflineSkipsMailWhenUserOnline() {
        User recipient = user(2L);
        recipient.setLastActiveAt(Instant.now());

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        notificationService.sendEmailIfOffline(2L, NotificationType.NEW_MESSAGE, "{\"content\":\"Salut\"}");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void notifyFriendRequestReceivedTargetsReceiver() {
        User sender = user(1L);
        User receiver = user(2L);
        FriendRequest friendRequest = friendRequest(5L, sender, receiver);

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyFriendRequestReceived(friendRequest);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isEqualTo(receiver);
        assertThat(notification.getType()).isEqualTo(NotificationType.FRIEND_REQUEST_RECEIVED);
        assertThat(notification.getPayload())
                .contains("\"friendRequestId\":5")
                .contains("\"senderId\":1")
                .contains("\"senderFirstName\":\"First1\"");
    }

    @Test
    void notifyFriendRequestAcceptedTargetsSender() {
        User sender = user(1L);
        User receiver = user(2L);
        FriendRequest friendRequest = friendRequest(5L, sender, receiver);
        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedAt(Instant.parse("2026-06-04T11:00:00Z"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyFriendRequestAccepted(friendRequest, 10L);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isEqualTo(sender);
        assertThat(notification.getType()).isEqualTo(NotificationType.FRIEND_REQUEST_ACCEPTED);
        assertThat(notification.getPayload())
                .contains("\"friendRequestId\":5")
                .contains("\"accepterId\":2")
                .contains("\"conversationId\":10");
    }

    @Test
    void notifyNewMessageTargetsOtherParticipant() {
        User sender = user(1L);
        User recipient = user(2L);
        Conversation conversation = conversation(10L, sender, recipient);
        Message message = message(100L, conversation, sender, "Bonjour");

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyNewMessage(message);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isEqualTo(recipient);
        assertThat(notification.getType()).isEqualTo(NotificationType.NEW_MESSAGE);
        assertThat(notification.getPayload())
                .contains("\"messageId\":100")
                .contains("\"conversationId\":10")
                .contains("\"content\":\"Bonjour\"");
    }

    @Test
    void notifyNewFileTargetsOtherParticipant() {
        User uploader = user(1L);
        User recipient = user(2L);
        Conversation conversation = conversation(10L, uploader, recipient);
        Attachment attachment = attachment(200L, conversation, uploader);

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyNewFile(attachment);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isEqualTo(recipient);
        assertThat(notification.getType()).isEqualTo(NotificationType.NEW_FILE);
        assertThat(notification.getPayload())
                .contains("\"attachmentId\":200")
                .contains("\"originalFileName\":\"note.txt\"");
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

    private Message message(Long id, Conversation conversation, User sender, String content) {
        return Message.builder()
                .id(id)
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .createdAt(Instant.parse("2026-06-04T10:00:00Z"))
                .edited(false)
                .deleted(false)
                .build();
    }

    private FriendRequest friendRequest(Long id, User sender, User receiver) {
        return FriendRequest.builder()
                .id(id)
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .requestedAt(Instant.parse("2026-06-04T10:00:00Z"))
                .build();
    }

    private Attachment attachment(Long id, Conversation conversation, User uploader) {
        return Attachment.builder()
                .id(id)
                .conversation(conversation)
                .uploader(uploader)
                .originalFileName("note.txt")
                .storageKey("opaque-key")
                .sizeBytes(12L)
                .contentType("text/plain")
                .uploadedAt(Instant.parse("2026-06-04T10:00:00Z"))
                .deleted(false)
                .build();
    }
}
