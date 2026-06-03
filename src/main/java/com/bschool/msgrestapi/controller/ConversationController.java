package com.bschool.msgrestapi.controller;

import com.bschool.msgrestapi.domain.entity.Message;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.dto.request.EditMessageRequest;
import com.bschool.msgrestapi.dto.request.SendMessageRequest;
import com.bschool.msgrestapi.dto.response.ConversationResponse;
import com.bschool.msgrestapi.service.ConversationService;
import com.bschool.msgrestapi.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations & Messages", description = "US1, US6, US10, US13")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @Operation(summary = "US1 — Lister les discussions triées par dernière activité")
    public List<ConversationResponse> listConversations(@CurrentUserId Long userId) {
        return conversationService.listForUser(userId)
            .stream()
            .map(c -> {
                User friend = c.getParticipantLow().getId().equals(userId)
                        ? c.getParticipantHigh()
                        : c.getParticipantLow();

                return ConversationResponse.builder()
                        .id(c.getId())
                        .friendId(friend.getId())
                        .friendFirstName(friend.getFirstName())
                        .friendLastName(friend.getLastName())
                        .lastActivityAt(c.getLastActivityAt())
                        .build();
            })
            .toList();
    }

    @GetMapping("/{conversationId}/messages")
    public List<Message> listMessages(
            @PathVariable Long conversationId,
            @CurrentUserId Long userId
    ) {
        return conversationService.listMessages(conversationId, userId);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "US6 — Envoyer un message (max 500 caractères)")
    public Message sendMessage(
            @PathVariable Long conversationId,
            @CurrentUserId Long userId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return conversationService.sendMessage(conversationId, userId, request.content());
    }

    @PutMapping("/messages/{messageId}")
    @Operation(summary = "US10 — Éditer un message")
    public Message editMessage(
            @PathVariable Long messageId,
            @CurrentUserId Long userId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        return conversationService.editMessage(messageId, userId, request.content());
    }

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "US13 — Supprimer un message")
    public Message deleteMessage(
            @PathVariable Long messageId,
            @CurrentUserId Long userId
    ) {
        return  conversationService.deleteMessage(messageId, userId);
    }
}
