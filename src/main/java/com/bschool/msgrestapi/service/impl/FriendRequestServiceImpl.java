package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.FriendRequestRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.FriendRequestService;
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

            return friendRequestRepository.save(friendRequest);
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
        throw new BusinessException("À implémenter — US5 + création discussion (Loïc / Charbel)");
    }

    @Override
    public FriendRequest decline(Long requestId, Long receiverId) {
        throw new BusinessException("À implémenter — US11 (Sabine)");
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
