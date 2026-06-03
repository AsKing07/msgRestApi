package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.config.MailProperties;
import com.bschool.msgrestapi.config.PresenceProperties;
import com.bschool.msgrestapi.domain.entity.Attachment;
import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Message;
import com.bschool.msgrestapi.domain.entity.Notification;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.NotificationType;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.NotificationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final PresenceProperties presenceProperties;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public Notification notifyUser(Long recipientId, NotificationType type, String payload) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .payload(payload)
                .createdAt(Instant.now())
                .emailSent(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void sendEmailIfOffline(Long recipientId, NotificationType type, String payload) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (isUserOnline(recipient)) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.from());
            message.setTo(recipient.getEmail());
            message.setSubject(buildEmailSubject(type));
            message.setText(buildEmailBody(type, payload));
            mailSender.send(message);

            markLatestNotificationEmailSent(recipientId, type);
            log.info("Email de notification {} envoyé à {}", type, recipient.getEmail());
        } catch (MailException ex) {
            log.warn("Impossible d'envoyer l'email à {} : {}", recipient.getEmail(), ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void notifyFriendRequestReceived(FriendRequest friendRequest) {
        String payload = buildFriendRequestPayload(friendRequest);
        Long receiverId = friendRequest.getReceiver().getId();

        notifyUser(receiverId, NotificationType.FRIEND_REQUEST_RECEIVED, payload);
        sendEmailIfOffline(receiverId, NotificationType.FRIEND_REQUEST_RECEIVED, payload);
    }

    @Override
    @Transactional
    public void notifyFriendRequestAccepted(FriendRequest friendRequest, Long conversationId) {
        String payload = buildFriendRequestAcceptedPayload(friendRequest, conversationId);
        Long senderId = friendRequest.getSender().getId();

        notifyUser(senderId, NotificationType.FRIEND_REQUEST_ACCEPTED, payload);
        sendEmailIfOffline(senderId, NotificationType.FRIEND_REQUEST_ACCEPTED, payload);
    }

    @Override
    @Transactional
    public void notifyNewMessage(Message message) {
        User sender = message.getSender();
        Conversation conversation = message.getConversation();
        Long recipientId = resolveRecipientId(conversation, sender.getId());

        String payload = buildNewMessagePayload(message);
        notifyUser(recipientId, NotificationType.NEW_MESSAGE, payload);
        sendEmailIfOffline(recipientId, NotificationType.NEW_MESSAGE, payload);
    }

    @Override
    @Transactional
    public void notifyNewFile(Attachment attachment) {
        User uploader = attachment.getUploader();
        Conversation conversation = attachment.getConversation();
        Long recipientId = resolveRecipientId(conversation, uploader.getId());

        String payload = buildNewFilePayload(attachment);
        notifyUser(recipientId, NotificationType.NEW_FILE, payload);
        sendEmailIfOffline(recipientId, NotificationType.NEW_FILE, payload);
    }

    @Override
    @Transactional
    public void notifyMessageDeleted(Message message) {
        User deleter = message.getSender();
        Conversation conversation = message.getConversation();
        Long recipientId = resolveRecipientId(conversation, deleter.getId());

        String payload = buildMessageDeletedPayload(message);
        notifyUser(recipientId, NotificationType.MESSAGE_DELETED, payload);
        sendEmailIfOffline(recipientId, NotificationType.MESSAGE_DELETED, payload);
    }

    private Long resolveRecipientId(Conversation conversation, Long senderId) {
        if (conversation.getParticipantLow().getId().equals(senderId)) {
            return conversation.getParticipantHigh().getId();
        }
        return conversation.getParticipantLow().getId();
    }

    private String buildNewMessagePayload(Message message) {
        User sender = message.getSender();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", message.getId());
        payload.put("conversationId", message.getConversation().getId());
        payload.put("senderId", sender.getId());
        payload.put("senderFirstName", sender.getFirstName());
        payload.put("senderLastName", sender.getLastName());
        payload.put("content", message.getContent());
        if (message.getCreatedAt() != null) {
            payload.put("sentAt", message.getCreatedAt().toString());
        }

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Impossible de sérialiser la notification", ex);
        }
    }

    private String buildNewFilePayload(Attachment attachment) {
        User uploader = attachment.getUploader();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attachmentId", attachment.getId());
        payload.put("conversationId", attachment.getConversation().getId());
        payload.put("uploaderId", uploader.getId());
        payload.put("uploaderFirstName", uploader.getFirstName());
        payload.put("uploaderLastName", uploader.getLastName());
        payload.put("originalFileName", attachment.getOriginalFileName());
        payload.put("sizeBytes", attachment.getSizeBytes());
        payload.put("contentType", attachment.getContentType());
        if (attachment.getUploadedAt() != null) {
            payload.put("uploadedAt", attachment.getUploadedAt().toString());
        }

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Impossible de sérialiser la notification", ex);
        }
    }

    private String buildMessageDeletedPayload(Message message) {
        User deleter = message.getSender();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", message.getId());
        payload.put("conversationId", message.getConversation().getId());
        payload.put("deletedById", deleter.getId());
        payload.put("deletedByFirstName", deleter.getFirstName());
        payload.put("deletedByLastName", deleter.getLastName());
        if (message.getDeletedAt() != null) {
            payload.put("deletedAt", message.getDeletedAt().toString());
        }

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Impossible de sérialiser la notification", ex);
        }
    }

    private boolean isUserOnline(User user) {
        if (user.getLastActiveAt() == null) {
            return false;
        }
        Instant threshold = Instant.now().minusSeconds(presenceProperties.offlineThresholdMinutes() * 60L);
        return user.getLastActiveAt().isAfter(threshold);
    }

    private void markLatestNotificationEmailSent(Long recipientId, NotificationType type) {
        notificationRepository.findByRecipientOrderByCreatedAtDesc(
                        userRepository.getReferenceById(recipientId)
                ).stream()
                .filter(n -> n.getType() == type && !n.isEmailSent())
                .findFirst()
                .ifPresent(notification -> {
                    notification.setEmailSent(true);
                    notificationRepository.save(notification);
                });
    }

    private String buildFriendRequestPayload(FriendRequest friendRequest) {
        User sender = friendRequest.getSender();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("friendRequestId", friendRequest.getId());
        payload.put("senderId", sender.getId());
        payload.put("senderFirstName", sender.getFirstName());
        payload.put("senderLastName", sender.getLastName());
        payload.put("requestedAt", friendRequest.getRequestedAt().toString());

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Impossible de sérialiser la notification", ex);
        }
    }

    private String buildFriendRequestAcceptedPayload(FriendRequest friendRequest, Long conversationId) {
        User accepter = friendRequest.getReceiver();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("friendRequestId", friendRequest.getId());
        payload.put("accepterId", accepter.getId());
        payload.put("accepterFirstName", accepter.getFirstName());
        payload.put("accepterLastName", accepter.getLastName());
        payload.put("conversationId", conversationId);
        payload.put("acceptedAt", friendRequest.getRespondedAt().toString());

        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Impossible de sérialiser la notification", ex);
        }
    }

    private String buildEmailSubject(NotificationType type) {
        return switch (type) {
            case FRIEND_REQUEST_RECEIVED -> "Nouvelle demande d'ami";
            case FRIEND_REQUEST_ACCEPTED -> "Demande d'ami acceptée";
            case NEW_MESSAGE -> "Nouveau message";
            case NEW_FILE -> "Nouveau fichier reçu";
            case MESSAGE_DELETED -> "Message supprimé dans une discussion";
            default -> "Notification Messagerie";
        };
    }

    private String buildEmailBody(NotificationType type, String payload) {
        if (type == NotificationType.FRIEND_REQUEST_RECEIVED) {
            return """
                    Bonjour,

                    Vous avez reçu une nouvelle demande d'ami.
                    Connectez-vous à l'application pour y répondre.

                    Détails : %s
                    """.formatted(payload);
        }
        if (type == NotificationType.FRIEND_REQUEST_ACCEPTED) {
            return """
                    Bonjour,

                    Votre demande d'ami a été acceptée.
                    Vous pouvez maintenant discuter avec cette personne.

                    Détails : %s
                    """.formatted(payload);
        }
        if (type == NotificationType.NEW_MESSAGE) {
            return """
                    Bonjour,

                    Vous avez reçu un nouveau message.
                    Connectez-vous à l'application pour le lire.

                    Détails : %s
                    """.formatted(payload);
        }
        if (type == NotificationType.NEW_FILE) {
            return """
                    Bonjour,

                    Vous avez reçu un nouveau fichier dans une discussion.
                    Connectez-vous à l'application pour le consulter.

                    Détails : %s
                    """.formatted(payload);
        }
        if (type == NotificationType.MESSAGE_DELETED) {
            return """
                    Bonjour,

                    Un message a été supprimé dans une de vos discussions.
                    Connectez-vous à l'application pour rester à jour.

                    Détails : %s
                    """.formatted(payload);
        }
        return payload;
    }
}
