package com.example.catalog.service;

import com.example.catalog.dto.UserDto;
import com.example.catalog.dto.auth.AuthenticatedUserDto;
import com.example.catalog.dto.auth.AuthResponse;
import com.example.catalog.dto.auth.LoginRequest;
import com.example.catalog.dto.auth.RegisterRequest;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserRole;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.exception.UnauthorizedException;
import com.example.catalog.mapper.UserMapper;
import com.example.catalog.repository.UserRepository;
import com.example.catalog.security.JwtService;
import com.example.catalog.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        ensureEmailAvailable(request.getEmail());
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(UserRole.USER);
        user.setBlocked(false);
        user.setProfilePrivate(false);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        return buildResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Неверная пара логин/пароль"));
        if (user.isBlocked()) {
            throw new UnauthorizedException("Учетная запись заблокирована");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Неверная пара логин/пароль");
        }
        return buildResponse(user);
    }

    public UserDto current(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Требуется авторизация");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.getId()));
        return UserMapper.toDto(UserMapper.toModel(user));
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user);
        Instant expiresAt = jwtService.extractExpiration(token);
        return AuthResponse.builder()
                .token(token)
                .expiresAt(expiresAt)
                .user(AuthenticatedUserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    private void ensureEmailAvailable(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
    }
}
