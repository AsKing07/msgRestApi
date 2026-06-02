package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.ActivityAuditLog;
import com.bschool.msgrestapi.domain.enums.AuditResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityAuditLogRepository extends JpaRepository<ActivityAuditLog, Long> {

    List<ActivityAuditLog> findByResourceTypeOrderByOccurredAtDesc(AuditResourceType resourceType);
}
