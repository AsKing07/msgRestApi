package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.domain.entity.Notification;
import com.bschool.msgrestapi.domain.enums.NotificationType;

public interface NotificationService {

    Notification notifyUser(Long recipientId, NotificationType type, String payload);

    void sendEmailIfOffline(Long recipientId, NotificationType type, String payload);
}
