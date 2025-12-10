package com.example.catalog.controller;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
@Tag(name = "Ratings", description = "Ratings operations")
public class RatingController {
    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add rating")
    public RatingDto add(@Valid @RequestBody RatingDto dto) {
        // добавляем оценку пользователя товару
        return ratingService.add(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update rating")
    public RatingDto update(@PathVariable Long id, @Valid @RequestBody RatingDto dto) {
        // обновляем оценку
        return ratingService.update(id, dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rating by id")
    public RatingDto get(@PathVariable Long id) {
        // получаем оценку по id
        return ratingService.get(id);
    }

    @GetMapping
    @Operation(summary = "Get all ratings")
    public List<RatingDto> getAll() {
        // возвращаем все оценки
        return ratingService.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete rating by id")
    public void delete(@PathVariable Long id) {
        // удаляем оценку
        ratingService.delete(id);
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Get ratings by user id")
    public List<RatingDto> byUser(@PathVariable("userId") Long userId) {
        // получаем оценки заданного пользователя
        return ratingService.byUser(userId);
    }

    @GetMapping("/by-item/{itemId}")
    @Operation(summary = "Get ratings by item id")
    public List<RatingDto> byItem(@PathVariable("itemId") Long itemId) {
        // получаем оценки для выбранного товара
        return ratingService.byItem(itemId);
    }
}
