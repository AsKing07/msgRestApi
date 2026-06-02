package com.bschool.msgrestapi.controller;

import com.bschool.msgrestapi.domain.entity.Notification;
import com.bschool.msgrestapi.repository.NotificationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "US4, US5, US7, US14, US17, US18, US19")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lister les notifications de l'utilisateur (polling ou complément WebSocket)")
    public List<Notification> list(@RequestHeader("X-User-Id") Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }
}
