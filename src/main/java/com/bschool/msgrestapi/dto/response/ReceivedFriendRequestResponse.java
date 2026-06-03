package com.bschool.msgrestapi.dto.response;

import com.bschool.msgrestapi.domain.entity.FriendRequest;

import java.time.Instant;

public record ReceivedFriendRequestResponse(
        Long requestId,
        Long senderId,
        String senderFirstName,
        String senderLastName,
        Instant requestedAt
) {
    public static ReceivedFriendRequestResponse from(FriendRequest friendRequest) {
        var sender = friendRequest.getSender();
        return new ReceivedFriendRequestResponse(
                friendRequest.getId(),
                sender.getId(),
                sender.getFirstName(),
                sender.getLastName(),
                friendRequest.getRequestedAt()
        );
    }
}
