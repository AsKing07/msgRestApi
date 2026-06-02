package com.bschool.msgrestapi.controller;

import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Friendship;
import com.bschool.msgrestapi.dto.request.SendFriendRequestDto;
import com.bschool.msgrestapi.service.FriendRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Tag(name = "Amis", description = "US2, US3, US5, US11, US12")
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "US2 — Envoyer une demande d'ami")
    public FriendRequest sendRequest(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SendFriendRequestDto request
    ) {
        return friendRequestService.sendRequest(userId, request.receiverId());
    }

    @GetMapping("/requests/received")
    @Operation(summary = "US3 — Lister les demandes reçues (plus récentes en premier)")
    public List<FriendRequest> listReceived(@RequestHeader("X-User-Id") Long userId) {
        return friendRequestService.listReceivedPending(userId);
    }

    @PostMapping("/requests/{requestId}/accept")
    @Operation(summary = "US5 — Accepter une demande (crée l'amitié + la discussion)")
    public FriendRequest accept(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return friendRequestService.accept(requestId, userId);
    }

    @PostMapping("/requests/{requestId}/decline")
    @Operation(summary = "US11 — Décliner une demande")
    public FriendRequest decline(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return friendRequestService.decline(requestId, userId);
    }

    @DeleteMapping("/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "US12 — Annuler une demande envoyée")
    public void cancel(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        friendRequestService.cancel(requestId, userId);
    }

    @GetMapping
    @Operation(summary = "US5 — Lister mes amis")
    public List<Friendship> listFriends(@RequestHeader("X-User-Id") Long userId) {
        return friendRequestService.listFriends(userId);
    }
}
