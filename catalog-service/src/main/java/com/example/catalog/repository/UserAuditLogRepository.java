package com.example.catalog.repository;

import com.example.catalog.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {
    List<UserAuditLog> findTop25ByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
}

