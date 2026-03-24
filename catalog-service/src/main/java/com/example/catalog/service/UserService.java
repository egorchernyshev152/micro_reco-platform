package com.example.catalog.service;

import com.example.catalog.dto.UserDto;
import com.example.catalog.dto.UserUpsertRequest;
import com.example.catalog.entity.User;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.UserMapper;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDto create(UserUpsertRequest request) {
        ensureEmailUnique(request.getEmail(), null);
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("Пароль обязателен для нового пользователя");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        return UserMapper.toDto(UserMapper.toModel(userRepository.save(user)));
    }

    public UserDto update(Long id, UserUpsertRequest request) {
        ensureEmailUnique(request.getEmail(), id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return UserMapper.toDto(UserMapper.toModel(userRepository.save(user)));
    }

    public UserDto get(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toModel)
                .map(UserMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toModel)
                .map(UserMapper::toDto)
                .toList();
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private void ensureEmailUnique(String email, Long currentId) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        boolean conflict = currentId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (conflict) {
            throw new ConflictException("Email already used: " + email);
        }
    }
}
