package com.example.catalog.repository;

import com.example.catalog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
