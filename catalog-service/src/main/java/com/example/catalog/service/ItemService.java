package com.example.catalog.service;

import com.example.catalog.dto.ItemDto;
import com.example.catalog.entity.Item;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.ItemMapper;
import com.example.catalog.model.ItemModel;
import com.example.catalog.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemDto create(ItemDto dto) {
        ItemModel model = ItemMapper.fromDto(dto);
        Item saved = itemRepository.save(ItemMapper.toEntity(model));
        return ItemMapper.toDto(ItemMapper.toModel(saved));
    }

    public ItemDto update(Long id, ItemDto dto) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Item not found: " + id));
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        return ItemMapper.toDto(ItemMapper.toModel(itemRepository.save(item)));
    }

    public ItemDto get(Long id) {
        return itemRepository.findById(id).map(ItemMapper::toModel).map(ItemMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Item not found: " + id));
    }

    public List<ItemDto> getAll() {
        return itemRepository.findAll().stream().map(ItemMapper::toModel).map(ItemMapper::toDto).toList();
    }

    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new NotFoundException("Item not found: " + id);
        }
        itemRepository.deleteById(id);
    }

    public List<ItemDto> findByIds(Collection<Long> ids) {
        return itemRepository.findByIdIn(ids).stream().map(ItemMapper::toModel).map(ItemMapper::toDto).toList();
    }

    public List<ItemDto> findByCategories(Collection<String> categories) {
        return itemRepository.findByCategoryIn(categories).stream().map(ItemMapper::toModel).map(ItemMapper::toDto).toList();
    }
}
