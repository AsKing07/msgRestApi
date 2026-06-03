package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.Friendship;
import com.bschool.msgrestapi.dto.response.ReceivedFriendRequestResponse;

import java.util.List;

public interface FriendRequestService {

    FriendRequest sendRequest(Long senderId, Long receiverId);

    List<ReceivedFriendRequestResponse> listReceivedPending(Long receiverId);

    FriendRequest accept(Long requestId, Long receiverId);

    FriendRequest decline(Long requestId, Long receiverId);

    void cancel(Long requestId, Long senderId);

    List<Friendship> listFriends(Long userId);
}
