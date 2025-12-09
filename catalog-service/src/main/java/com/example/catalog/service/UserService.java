package com.example.catalog.service;

import com.example.catalog.dto.UserDto;
import com.example.catalog.entity.User;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.UserMapper;
import com.example.catalog.model.UserModel;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDto create(UserDto dto) {
        ensureEmailUnique(dto.getEmail(), null);
        UserModel model = UserMapper.fromDto(dto);
        User saved = userRepository.save(UserMapper.toEntity(model));
        return UserMapper.toDto(UserMapper.toModel(saved));
    }

    public UserDto update(Long id, UserDto dto) {
        ensureEmailUnique(dto.getEmail(), id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return UserMapper.toDto(UserMapper.toModel(userRepository.save(user)));
    }

    public UserDto get(Long id) {
        return userRepository.findById(id).map(UserMapper::toModel).map(UserMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toModel).map(UserMapper::toDto).toList();
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private void ensureEmailUnique(String email, Long currentId) {
        if (email == null) return;
        boolean conflict = currentId == null
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, currentId);
        if (conflict) {
            throw new ConflictException("Email already used: " + email);
        }
    }
}
