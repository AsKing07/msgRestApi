package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.Notification;
import com.bschool.msgrestapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
}
