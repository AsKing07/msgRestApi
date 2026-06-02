package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.domain.entity.ActivityAuditLog;
import com.bschool.msgrestapi.domain.enums.AuditAction;
import com.bschool.msgrestapi.domain.enums.AuditResourceType;

import java.util.List;

public interface AuditService {

    ActivityAuditLog log(Long actorId, AuditResourceType resourceType, AuditAction action, Long resourceId, String metadata);

    List<ActivityAuditLog> listMessageLogs();

    List<ActivityAuditLog> listFileLogs();
}
