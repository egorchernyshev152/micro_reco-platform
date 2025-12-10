package com.example.catalog.controller;

import com.example.catalog.dto.UserDto;
import com.example.catalog.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User CRUD operations")
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    public UserDto create(@Valid @RequestBody UserDto dto) {
        // создаем пользователя
        return userService.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public UserDto update(@PathVariable("id") Long id, @Valid @RequestBody UserDto dto) {
        // обновляем данные пользователя
        return userService.update(id, dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public UserDto get(@PathVariable("id") Long id) {
        // получаем пользователя по идентификатору
        return userService.get(id);
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public List<UserDto> getAll() {
        // возвращаем всех пользователей
        return userService.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user")
    public void delete(@PathVariable("id") Long id) {
        // удаляем пользователя
        userService.delete(id);
    }
}
