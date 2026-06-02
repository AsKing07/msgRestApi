package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.service.AuditService;
import com.bschool.msgrestapi.domain.entity.ActivityAuditLog;
import com.bschool.msgrestapi.domain.enums.AuditAction;
import com.bschool.msgrestapi.domain.enums.AuditResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditServiceImpl implements AuditService {

    @Override
    public ActivityAuditLog log(Long actorId, AuditResourceType resourceType, AuditAction action, Long resourceId, String metadata) {
        throw new BusinessException("À implémenter — US15/US16 (Dandara)");
    }

    @Override
    public List<ActivityAuditLog> listMessageLogs() {
        throw new BusinessException("À implémenter — US15 (Dandara)");
    }

    @Override
    public List<ActivityAuditLog> listFileLogs() {
        throw new BusinessException("À implémenter — US16 (Dandara)");
    }
}
