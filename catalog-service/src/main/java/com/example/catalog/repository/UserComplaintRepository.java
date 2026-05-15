package com.example.catalog.repository;

import com.example.catalog.entity.UserComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserComplaintRepository extends JpaRepository<UserComplaint, Long> {

    List<UserComplaint> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    @Query("""
            SELECT c.targetUser.id, COUNT(c)
            FROM UserComplaint c
            WHERE c.targetUser.id IN :userIds AND c.status <> com.example.catalog.entity.ComplaintStatus.RESOLVED
            GROUP BY c.targetUser.id
            """)
    List<Object[]> countActiveComplaintsByTarget(@Param("userIds") Collection<Long> userIds);

    Optional<UserComplaint> findByIdAndTargetUserId(Long id, Long targetUserId);
}
