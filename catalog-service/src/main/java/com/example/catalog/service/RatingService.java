package com.example.catalog.service;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.entity.Item;
import com.example.catalog.entity.Rating;
import com.example.catalog.entity.User;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.RatingMapper;
import com.example.catalog.model.RatingModel;
import com.example.catalog.repository.ItemRepository;
import com.example.catalog.repository.RatingRepository;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public RatingDto add(RatingDto dto) {
        ensureUnique(dto.getUserId(), dto.getItemId(), null);
        RatingModel model = RatingMapper.fromDto(dto);
        Rating saved = ratingRepository.save(toEntityWithRelations(model));
        return RatingMapper.toDto(RatingMapper.toModel(saved));
    }

    public RatingDto update(Long id, RatingDto dto) {
        Rating existing = ratingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rating not found: " + id));
        ensureUnique(dto.getUserId(), dto.getItemId(), id);
        RatingModel fromDto = RatingMapper.fromDto(dto);
        RatingModel model = RatingModel.builder()
                .id(id)
                .userId(fromDto.getUserId())
                .itemId(fromDto.getItemId())
                .score(fromDto.getScore())
                .createdAt(existing.getCreatedAt())
                .build();
        Rating saved = ratingRepository.save(toEntityWithRelations(model));
        return RatingMapper.toDto(RatingMapper.toModel(saved));
    }

    public List<RatingDto> byUser(Long userId) {
        return ratingRepository.findByUser_Id(userId).stream().map(RatingMapper::toModel).map(RatingMapper::toDto).toList();
    }

    public List<RatingDto> byItem(Long itemId) {
        return ratingRepository.findByItem_Id(itemId).stream().map(RatingMapper::toModel).map(RatingMapper::toDto).toList();
    }

    public RatingDto get(Long id) {
        return ratingRepository.findById(id)
                .map(RatingMapper::toModel)
                .map(RatingMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Rating not found: " + id));
    }

    public List<RatingDto> getAll() {
        return ratingRepository.findAll().stream().map(RatingMapper::toModel).map(RatingMapper::toDto).toList();
    }

    public void delete(Long id) {
        if (!ratingRepository.existsById(id)) {
            throw new NotFoundException("Rating not found: " + id);
        }
        ratingRepository.deleteById(id);
    }

    private Rating toEntityWithRelations(RatingModel model) {
        User user = userRepository.findById(model.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + model.getUserId()));
        Item item = itemRepository.findById(model.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found: " + model.getItemId()));
        return RatingMapper.toEntity(model, user, item);
    }

    private void ensureUnique(Long userId, Long itemId, Long currentId) {
        if (userId == null || itemId == null) return;
        boolean conflict = currentId == null
                ? ratingRepository.existsByUser_IdAndItem_Id(userId, itemId)
                : ratingRepository.existsByUser_IdAndItem_IdAndIdNot(userId, itemId, currentId);
        if (conflict) {
            throw new ConflictException("Rating already exists for user " + userId + " and item " + itemId);
        }
    }
}
