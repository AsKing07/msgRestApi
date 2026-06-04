package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.ActivityAuditLogRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.AuditService;
import com.bschool.msgrestapi.domain.entity.ActivityAuditLog;
import com.bschool.msgrestapi.domain.enums.AuditAction;
import com.bschool.msgrestapi.domain.enums.AuditResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final ActivityAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    public ActivityAuditLog log(Long actorId, AuditResourceType resourceType, AuditAction action, Long resourceId, String metadata) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouvé : " + actorId));

        ActivityAuditLog auditLog = ActivityAuditLog.builder()
                .actor(actor)
                .resourceType(resourceType)
                .action(action)
                .resourceId(resourceId)
                .occurredAt(Instant.now())
                .metadata(metadata)
                .build();

        return auditLogRepository.save(auditLog);
    }

    @Override
    public List<ActivityAuditLog> listMessageLogs() {
        return auditLogRepository.findByResourceTypeOrderByOccurredAtDesc(AuditResourceType.MESSAGE);
    }

    @Override
    public List<ActivityAuditLog> listFileLogs() {
        return auditLogRepository.findByResourceTypeOrderByOccurredAtDesc(AuditResourceType.FILE);
    }
}
