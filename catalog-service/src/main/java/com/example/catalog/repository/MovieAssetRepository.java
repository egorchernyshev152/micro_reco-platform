package com.example.catalog.repository;

import com.example.catalog.entity.MovieAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieAssetRepository extends JpaRepository<MovieAsset, Long> {
}
