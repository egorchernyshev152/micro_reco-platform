package com.example.catalog.service;

import com.example.catalog.dto.UserDto;
import com.example.catalog.entity.User;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDto create(UserDto dto) {
        User user = User.builder().name(dto.getName()).email(dto.getEmail()).build();
        user = userRepository.save(user);
        return toDto(user);
    }

    public UserDto update(Long id, UserDto dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return toDto(userRepository.save(user));
    }

    public UserDto get(Long id) {
        return userRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}

