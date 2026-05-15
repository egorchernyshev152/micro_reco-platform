package com.example.catalog.repository;

import com.example.catalog.entity.User;
import com.example.catalog.entity.UserRole;
import com.example.catalog.repository.spec.UserSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.saveAll(List.of(
                buildUser("Admin", "admin@example.com", UserRole.ADMIN, false),
                buildUser("Blocked User", "blocked@example.com", UserRole.USER, true),
                buildUser("Active User", "user@example.com", UserRole.USER, false)
        ));
    }

    @Test
    @DisplayName("UserSpecifications поддерживает фильтры по тексту, роли и блокировке")
    void shouldFilterByQueryRoleAndBlockedFlag() {
        Specification<User> spec = UserSpecifications.withFilters("user", UserRole.USER, false);
        List<User> result = userRepository.findAll(spec);

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(User::getEmail)
                .isEqualTo("user@example.com");
    }

    private User buildUser(String name, String email, UserRole role, boolean blocked) {
        return User.builder()
                .name(name)
                .email(email)
                .role(role)
                .blocked(blocked)
                .passwordHash("hash")
                .build();
    }
}
