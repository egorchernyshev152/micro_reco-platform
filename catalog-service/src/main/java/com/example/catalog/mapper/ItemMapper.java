package com.example.catalog.mapper;

import com.example.catalog.dto.ItemDto;
import com.example.catalog.entity.Item;
import com.example.catalog.model.ItemModel;

public final class ItemMapper {
    private ItemMapper() {
    }

    public static ItemModel toModel(Item entity) {
        // переводим entity JPA в внутреннюю модель
        if (entity == null) return null;
        return ItemModel.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .build();
    }

    public static Item toEntity(ItemModel model) {
        // собираем entity из модели для сохранения
        if (model == null) return null;
        return Item.builder()
                .id(model.getId())
                .title(model.getTitle())
                .description(model.getDescription())
                .category(model.getCategory())
                .build();
    }

    public static ItemModel fromDto(ItemDto dto) {
        // конвертируем входной DTO в модель
        if (dto == null) return null;
        return ItemModel.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .build();
    }

    public static ItemDto toDto(ItemModel model) {
        // конвертируем модель в DTO для выдачи наружу
        if (model == null) return null;
        return ItemDto.builder()
                .id(model.getId())
                .title(model.getTitle())
                .description(model.getDescription())
                .category(model.getCategory())
                .build();
    }
}
