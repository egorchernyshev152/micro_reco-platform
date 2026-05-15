package com.example.catalog.service;

import com.example.catalog.dto.UserDto;
import com.example.catalog.dto.UserUpsertRequest;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserRole;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createShouldEncodePasswordAndPersist() {
        UserUpsertRequest request = buildUpsertRequest("alice@mail.com", "Pass1234!", UserRole.ADMIN);
        when(userRepository.existsByEmailIgnoreCase("alice@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            return user;
        });

        UserDto result = userService.create(request);

        assertThat(result.getId()).isEqualTo(42L);
        verify(userRepository).save(argThat(user -> "encoded".equals(user.getPasswordHash()) && user.getRole() == UserRole.ADMIN));
    }

    @Test
    void createShouldFailOnDuplicateEmail() {
        UserUpsertRequest request = buildUpsertRequest("exists@mail.com", "Pass1234!", UserRole.USER);
        when(userRepository.existsByEmailIgnoreCase("exists@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateShouldAllowChangingRoleAndPassword() {
        UserUpsertRequest request = buildUpsertRequest("new@mail.com", "AnotherPass1", UserRole.ADMIN);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@mail.com", 7L)).thenReturn(false);
        User existing = userEntity(7L, "Old", "old@mail.com", UserRole.USER, "hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("AnotherPass1")).thenReturn("fresh-hash");
        when(userRepository.save(existing)).thenReturn(existing);

        UserDto updated = userService.update(7L, request);

        assertThat(updated.getEmail()).isEqualTo("new@mail.com");
        assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
        verify(passwordEncoder).encode("AnotherPass1");
    }

    @Test
    void updateShouldThrowWhenUserMissing() {
        UserUpsertRequest request = buildUpsertRequest("none@mail.com", "SecretPass1", UserRole.USER);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("none@mail.com", 99L)).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getShouldMapToDto() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(userEntity(5L, "Rick", "rick@mail.com", UserRole.USER, "hash")));

        UserDto dto = userService.get(5L);

        assertThat(dto.getName()).isEqualTo("Rick");
        assertThat(dto.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void listShouldReturnAll() {
        when(userRepository.findAll()).thenReturn(List.of(
                userEntity(1L, "A", "a@mail.com", UserRole.USER, "x"),
                userEntity(2L, "B", "b@mail.com", UserRole.ADMIN, "y")
        ));

        List<UserDto> items = userService.getAll();

        assertThat(items).hasSize(2);
        assertThat(items).extracting(UserDto::getRole).contains(UserRole.ADMIN, UserRole.USER);
    }

    private UserUpsertRequest buildUpsertRequest(String email, String password, UserRole role) {
        UserUpsertRequest request = new UserUpsertRequest();
        request.setName("Name");
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);
        return request;
    }

    private User userEntity(Long id, String name, String email, UserRole role, String passwordHash) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .role(role)
                .passwordHash(passwordHash)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
