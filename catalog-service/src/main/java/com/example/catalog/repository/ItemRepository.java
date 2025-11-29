package com.example.catalog.repository;

import com.example.catalog.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByIdIn(Collection<Long> ids);
    List<Item> findByCategoryIn(Collection<String> categories);
}

