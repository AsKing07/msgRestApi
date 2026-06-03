package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.FriendRequest;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByReceiverAndStatusOrderByRequestedAtDesc(User receiver, FriendRequestStatus status);

    @Query("""
            SELECT fr FROM FriendRequest fr
            JOIN FETCH fr.sender
            WHERE fr.receiver = :receiver AND fr.status = :status
            ORDER BY fr.requestedAt DESC
            """)
    List<FriendRequest> findReceivedPendingWithSender(
            @Param("receiver") User receiver,
            @Param("status") FriendRequestStatus status
    );

    List<FriendRequest> findBySenderAndStatusOrderByRequestedAtDesc(User sender, FriendRequestStatus status);

    Optional<FriendRequest> findBySenderAndReceiverAndStatus(User sender, User receiver, FriendRequestStatus status);

    Optional<FriendRequest> findBySender_IdAndReceiver_Id(Long senderId, Long receiverId);

    @Query("""
            SELECT fr FROM FriendRequest fr
            WHERE fr.status = :status
            AND ((fr.sender.id = :userId1 AND fr.receiver.id = :userId2)
                 OR (fr.sender.id = :userId2 AND fr.receiver.id = :userId1))
            """)
    Optional<FriendRequest> findPendingBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            @Param("status") FriendRequestStatus status
    );
}
