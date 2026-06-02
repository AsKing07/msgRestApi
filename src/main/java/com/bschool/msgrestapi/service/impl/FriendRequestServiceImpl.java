package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.FriendRequestRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.FriendRequestService;
import com.bschool.msgrestapi.service.NotificationService;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Friendship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendRequestServiceImpl implements FriendRequestService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    FriendRequestRepository friendRequestRepository;
    @Autowired
    NotificationService notificationService;

    @Override
    public FriendRequest sendRequest(Long senderId, Long receiverId) {
        //CRÉER LA DEMANDE D'AMI
        try {
            FriendRequest friendRequest = new FriendRequest();
            FriendRequest friendRequest2 = new FriendRequest();
            friendRequest2 = friendRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId);
            if(friendRequest2 != null){
                throw new BusinessException("Une demande d'ami existe déjà entre ces deux utilisateurs.");
            }
            User sender = userRepository.getOne(senderId);
            User receiver = userRepository.getOne(receiverId);
            friendRequest.setSender(sender);
            friendRequest.setReceiver(receiver);
            friendRequest.setStatus(FriendRequestStatus.PENDING);
            friendRequest.setRequestedAt(java.time.Instant.now());

            FriendRequest saved = friendRequestRepository.save(friendRequest);
            notificationService.notifyFriendRequestReceived(saved);
            return saved;
        }catch (Exception e){
            throw new BusinessException("Erreur lors de l'envoi de la demande d'ami : " + e.getMessage());
        }
    }

    @Override
    public List<FriendRequest> listReceivedPending(Long receiverId) {
        throw new BusinessException("À implémenter — US3 (Sabine)");
    }

    @Override
    public FriendRequest accept(Long requestId, Long receiverId) {
        FriendRequest friendRequest = friendRequestRepository.getOne(requestId);
        if(!friendRequest.getReceiver().getId().equals(receiverId)){
            throw new BusinessException("L'utilisateur n'est pas le destinataire de la demande d'ami");
        }
        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedAt(java.time.Instant.now());
        return friendRequestRepository.save(friendRequest);
    }

    @Override
    public FriendRequest decline(Long requestId, Long receiverId) {
        FriendRequest friendRequest = friendRequestRepository.getReferenceById(requestId);
        if (!friendRequest.getReceiver().getId().equals(receiverId)) {
            throw new BusinessException("L'utilisateur n'est pas le destinataire de la demande d'ami");
        }
        friendRequest.setStatus(FriendRequestStatus.DECLINED);
        friendRequest.setDeclineAt(java.time.Instant.now());
        return friendRequestRepository.save(friendRequest);
        //throw new BusinessException("À implémenter — US11 (Sabine)");
    }

    @Override
    public void cancel(Long requestId, Long senderId) {
        throw new BusinessException("À implémenter — US12 (Dandara)");
    }

    @Override
    public List<Friendship> listFriends(Long userId) {
        throw new BusinessException("À implémenter — US5 (Loïc)");
    }
}
