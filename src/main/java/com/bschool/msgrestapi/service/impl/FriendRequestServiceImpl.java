package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.service.FriendRequestService;
import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Friendship;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendRequestServiceImpl implements FriendRequestService {

    @Override
    public FriendRequest sendRequest(Long senderId, Long receiverId) {
        throw new BusinessException("À implémenter — US2 (Loïc)");
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
