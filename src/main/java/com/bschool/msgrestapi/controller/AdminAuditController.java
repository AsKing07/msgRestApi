package com.bschool.msgrestapi.controller;

import com.bschool.msgrestapi.domain.entity.ActivityAuditLog;
import com.bschool.msgrestapi.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "US15, US16")
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping("/messages")
    @Operation(summary = "US15 — Logs de chaque message")
    public List<ActivityAuditLog> messageLogs() {
        return auditService.listMessageLogs();
    }

    @GetMapping("/files")
    @Operation(summary = "US16 — Logs de chaque fichier")
    public List<ActivityAuditLog> fileLogs() {
        return auditService.listFileLogs();
    }
}
