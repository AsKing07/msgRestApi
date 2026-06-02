package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByReceiverAndStatusOrderByRequestedAtDesc(User receiver, FriendRequestStatus status);

    List<FriendRequest> findBySenderAndStatusOrderByRequestedAtDesc(User sender, FriendRequestStatus status);

    Optional<FriendRequest> findBySenderAndReceiverAndStatus(User sender, User receiver, FriendRequestStatus status);

    FriendRequest findBySenderIdAndReceiverId(Long senderId, Long receiverId);
}
