package com.example.catalog.repository;

import com.example.catalog.entity.ReviewModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewModerationLogRepository extends JpaRepository<ReviewModerationLog, Long> {
}
