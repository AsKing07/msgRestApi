package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.config.MailProperties;
import com.bschool.msgrestapi.config.PresenceProperties;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Notification;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.NotificationType;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.NotificationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ObjectMapper objectMapper;

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
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossible de sérialiser la notification", ex);
        }
    }

    private String buildEmailSubject(NotificationType type) {
        return switch (type) {
            case FRIEND_REQUEST_RECEIVED -> "Nouvelle demande d'ami";
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
        return payload;
    }
}
