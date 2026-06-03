package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Friendship;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import com.bschool.msgrestapi.domain.util.UserPairUtil;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.FriendRequestRepository;
import com.bschool.msgrestapi.repository.FriendshipRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.ConversationService;
import com.bschool.msgrestapi.service.FriendRequestService;
import com.bschool.msgrestapi.service.NotificationService;
import com.bschool.msgrestapi.dto.response.ReceivedFriendRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final ConversationService conversationService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FriendRequest sendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException("Vous ne pouvez pas vous ajouter vous-même en ami.");
        }

        User sender = requireUser(senderId);
        User receiver = requireUser(receiverId);
        var orderedUsers = UserPairUtil.order(sender, receiver);

        if (friendshipRepository.findByUserLowAndUserHigh(orderedUsers.low(), orderedUsers.high()).isPresent()) {
            throw new BusinessException("Vous êtes déjà amis avec cet utilisateur.");
        }

        Optional<FriendRequest> existingPending = friendRequestRepository.findPendingBetweenUsers(
                senderId,
                receiverId,
                FriendRequestStatus.PENDING
        );

        if (existingPending.isPresent()) {
            FriendRequest existing = existingPending.get();
            if (existing.getSender().getId().equals(senderId)) {
                throw new BusinessException("Vous avez déjà envoyé une demande d'ami à cet utilisateur.");
            }
            throw new BusinessException(
                    "Cet utilisateur vous a déjà envoyé une demande d'ami. Acceptez-la pour devenir amis."
            );
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        FriendRequest saved = friendRequestRepository.save(friendRequest);
        notificationService.notifyFriendRequestReceived(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedFriendRequestResponse> listReceivedPending(Long receiverId) {
        User receiver = requireUser(receiverId);
        return friendRequestRepository
                .findReceivedPendingWithSender(receiver, FriendRequestStatus.PENDING)
                .stream()
                .map(ReceivedFriendRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public FriendRequest accept(Long requestId, Long receiverId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande d'ami introuvable."));

        if (!friendRequest.getReceiver().getId().equals(receiverId)) {
            throw new BusinessException("L'utilisateur n'est pas le destinataire de la demande d'ami.");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException("Cette demande d'ami a déjà été traitée.");
        }

        Instant now = Instant.now();
        User sender = friendRequest.getSender();
        User receiver = friendRequest.getReceiver();
        var orderedUsers = UserPairUtil.order(sender, receiver);

        friendshipRepository.findByUserLowAndUserHigh(orderedUsers.low(), orderedUsers.high())
                .orElseGet(() -> friendshipRepository.save(Friendship.builder()
                        .userLow(orderedUsers.low())
                        .userHigh(orderedUsers.high())
                        .createdAt(now)
                        .build()));

        Conversation conversation = conversationService.getOrCreateBetweenFriends(
                sender.getId(),
                receiver.getId()
        );

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedAt(now);
        FriendRequest saved = friendRequestRepository.save(friendRequest);

        cancelReversePendingRequest(sender.getId(), receiver.getId(), saved.getId());

        notificationService.notifyFriendRequestAccepted(saved, conversation.getId());
        return saved;
    }

    /** Annule une éventuelle demande réciproque encore en attente (ex. B→A quand on accepte A→B). */
    private void cancelReversePendingRequest(Long senderId, Long receiverId, Long acceptedRequestId) {
        friendRequestRepository.findPendingBetweenUsers(senderId, receiverId, FriendRequestStatus.PENDING)
                .filter(fr -> !fr.getId().equals(acceptedRequestId))
                .ifPresent(reverse -> {
                    reverse.setStatus(FriendRequestStatus.CANCELLED);
                    reverse.setRespondedAt(Instant.now());
                    friendRequestRepository.save(reverse);
                });
    }

    @Override
    @Transactional
    public FriendRequest decline(Long requestId, Long receiverId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande d'ami introuvable."));

        if (!friendRequest.getReceiver().getId().equals(receiverId)) {
            throw new BusinessException("L'utilisateur n'est pas le destinataire de la demande d'ami.");
        }

        friendRequest.setStatus(FriendRequestStatus.DECLINED);
        friendRequest.setDeclineAt(Instant.now());
        return friendRequestRepository.save(friendRequest);
    }

    @Override
    @Transactional
    public void cancel(Long requestId, Long senderId) {
      Optional<FriendRequest> friendRequest = friendRequestRepository.findById(requestId);
       if (!friendRequest.get().getSender().getId().equals(senderId)) {
           throw new BusinessException("Vous n'êtes pas l'auteur de la demande");
       }
        friendRequest.get().setStatus(FriendRequestStatus.CANCELLED);
         friendRequestRepository.save(friendRequest.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Friendship> listFriends(Long userId) {
        User user = requireUser(userId);
        return friendshipRepository.findAllByUser(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }
}
