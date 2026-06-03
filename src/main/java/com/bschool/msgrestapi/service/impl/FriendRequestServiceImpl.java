package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Friendship;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import com.bschool.msgrestapi.domain.util.UserPairUtil;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.FriendRequestRepository;
import com.bschool.msgrestapi.repository.FriendshipRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.FriendRequestService;
import com.bschool.msgrestapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final ConversationRepository conversationRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FriendRequest sendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException("Vous ne pouvez pas vous ajouter vous-même en ami.");
        }

        User sender = requireUser(senderId);
        User receiver = requireUser(receiverId);

        if (friendRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId) != null) {
            throw new BusinessException("Une demande d'ami existe déjà entre ces deux utilisateurs.");
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
    public List<FriendRequest> listReceivedPending(Long receiverId) {
        throw new BusinessException("À implémenter — US3 (Sabine)");
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

        Conversation conversation = conversationRepository
                .findByParticipantLowAndParticipantHigh(orderedUsers.low(), orderedUsers.high())
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .participantLow(orderedUsers.low())
                        .participantHigh(orderedUsers.high())
                        .createdAt(now)
                        .lastActivityAt(now)
                        .build()));

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedAt(now);
        FriendRequest saved = friendRequestRepository.save(friendRequest);

        notificationService.notifyFriendRequestAccepted(saved, conversation.getId());
        return saved;
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
        throw new BusinessException("À implémenter — US12 (Dandara)");
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
