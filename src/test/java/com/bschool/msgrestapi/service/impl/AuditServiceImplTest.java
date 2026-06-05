package com.bschool.msgrestapi.service.impl;


import com.bschool.msgrestapi.domain.entity.ActivityAuditLog;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.enums.AuditAction;
import com.bschool.msgrestapi.domain.enums.AuditResourceType;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.repository.ActivityAuditLogRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private ActivityAuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    void log_shouldSaveAuditLog_whenUserExists() {
        // GIVEN — on prépare les données
        User actor = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Martin")
                .email("alice@test.com")
                .passwordHash("hash")
                .build();

        ActivityAuditLog expectedLog = ActivityAuditLog.builder()
                .actor(actor)
                .resourceType(AuditResourceType.MESSAGE)
                .action(AuditAction.SENT)
                .resourceId(42L)
                .metadata("Hello World")
                .occurredAt(Instant.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(auditLogRepository.save(any())).thenReturn(expectedLog);

        // WHEN — on appelle la méthode
        ActivityAuditLog result = auditService.log(1L, AuditResourceType.MESSAGE, AuditAction.SENT, 42L, "Hello World");

        // THEN — on vérifie le résultat
        assertThat(result.getActor()).isEqualTo(actor);
        assertThat(result.getResourceType()).isEqualTo(AuditResourceType.MESSAGE);
        assertThat(result.getAction()).isEqualTo(AuditAction.SENT);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void log_shouldThrowException_whenUserNotFound() {
        // GIVEN
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN + THEN
        assertThatThrownBy(() -> auditService.log(99L, AuditResourceType.MESSAGE, AuditAction.SENT, 1L, "test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listMessageLogs_shouldReturnOnlyMessageLogs() {
        // GIVEN
        List<ActivityAuditLog> logs = List.of(
                ActivityAuditLog.builder().resourceType(AuditResourceType.MESSAGE).build()
        );
        when(auditLogRepository.findByResourceTypeOrderByOccurredAtDesc(AuditResourceType.MESSAGE))
                .thenReturn(logs);

        // WHEN
        List<ActivityAuditLog> result = auditService.listMessageLogs();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResourceType()).isEqualTo(AuditResourceType.MESSAGE);
    }

    @Test
    void listFileLogs_shouldReturnOnlyFileLogs() {
        // GIVEN
        List<ActivityAuditLog> logs = List.of(
                ActivityAuditLog.builder().resourceType(AuditResourceType.FILE).build()
        );
        when(auditLogRepository.findByResourceTypeOrderByOccurredAtDesc(AuditResourceType.FILE))
                .thenReturn(logs);

        // WHEN
        List<ActivityAuditLog> result = auditService.listFileLogs();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResourceType()).isEqualTo(AuditResourceType.FILE);
    }
}
