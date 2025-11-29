package com.example.catalog.service;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.entity.Item;
import com.example.catalog.entity.Rating;
import com.example.catalog.entity.User;
import com.example.catalog.exception.NotFoundException;
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
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + dto.getUserId()));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found: " + dto.getItemId()));

        Rating rating = Rating.builder()
                .user(user)
                .item(item)
                .score(dto.getScore())
                .build();
        rating = ratingRepository.save(rating);
        return toDto(rating);
    }

    public List<RatingDto> byUser(Long userId) {
        return ratingRepository.findByUser_Id(userId).stream().map(this::toDto).toList();
    }

    public List<RatingDto> byItem(Long itemId) {
        return ratingRepository.findByItem_Id(itemId).stream().map(this::toDto).toList();
    }

    private RatingDto toDto(Rating rating) {
        return RatingDto.builder()
                .id(rating.getId())
                .userId(rating.getUser().getId())
                .itemId(rating.getItem().getId())
                .score(rating.getScore())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}

