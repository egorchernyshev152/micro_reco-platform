package com.example.catalog.controller;

import com.example.catalog.dto.ItemDto;
import com.example.catalog.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Item CRUD operations and search")
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create item")
    public ItemDto create(@Valid @RequestBody ItemDto dto) {
        return itemService.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update item")
    public ItemDto update(@PathVariable("id") Long id, @Valid @RequestBody ItemDto dto) {
        return itemService.update(id, dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by id")
    public ItemDto get(@PathVariable("id") Long id) {
        return itemService.get(id);
    }

    @GetMapping
    @Operation(summary = "Get all items")
    public List<ItemDto> getAll() {
        return itemService.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete item")
    public void delete(@PathVariable("id") Long id) {
        itemService.delete(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Find items by ids")
    public List<ItemDto> findByIds(@RequestParam("ids") Collection<Long> ids) {
        return itemService.findByIds(ids);
    }

    @GetMapping("/by-categories")
    @Operation(summary = "Find items by categories")
    public List<ItemDto> findByCategories(@RequestParam("categories") Collection<String> categories) {
        return itemService.findByCategories(categories);
    }
}
