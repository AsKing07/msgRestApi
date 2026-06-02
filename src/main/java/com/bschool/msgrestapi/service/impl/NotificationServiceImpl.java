package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.service.NotificationService;
import com.bschool.msgrestapi.domain.entity.Notification;
import com.bschool.msgrestapi.domain.enums.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public Notification notifyUser(Long recipientId, NotificationType type, String payload) {
        throw new BusinessException("À implémenter — US4, US5, US7, US14, US17-19 (Charbel + Ivan)");
    }

    @Override
    public void sendEmailIfOffline(Long recipientId, NotificationType type, String payload) {
        throw new BusinessException("À implémenter — US4 email hors ligne (Charbel)");
    }
}
