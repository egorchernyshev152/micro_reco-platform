package com.example.catalog.service;

import com.example.catalog.dto.MovieAssetDto;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.MovieAsset;
import com.example.catalog.entity.MovieAssetType;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.MovieAssetMapper;
import com.example.catalog.repository.MovieAssetRepository;
import com.example.catalog.repository.MovieRepository;
import com.example.catalog.service.asset.MovieAssetStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MovieAssetService {

    private final MovieRepository movieRepository;
    private final MovieAssetRepository movieAssetRepository;
    private final MovieAssetStorage movieAssetStorage;

    @Transactional
    public MovieAssetDto upload(Long movieId, MovieAssetType type, MultipartFile file, String label) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found: " + movieId));
        MovieAssetType resolvedType = type != null ? type : MovieAssetType.GALLERY;
        MovieAssetStorage.StoredMovieAsset stored = movieAssetStorage.store(movieId, resolvedType, file);
        MovieAsset asset = MovieAsset.builder()
                .movie(movie)
                .type(resolvedType)
                .fileName(file != null ? file.getOriginalFilename() : null)
                .contentType(file != null ? file.getContentType() : null)
                .fileSize(file != null ? file.getSize() : null)
                .objectKey(stored.objectKey())
                .publicUrl(stored.url())
                .storage(movieAssetStorage.getType().name())
                .label(StringUtils.hasText(label) ? label : null)
                .build();
        MovieAsset saved = movieAssetRepository.save(asset);
        updateMoviePrimaryAsset(movie, resolvedType, stored.url());
        return MovieAssetMapper.toDto(saved);
    }

    private void updateMoviePrimaryAsset(Movie movie, MovieAssetType type, String url) {
        switch (type) {
            case POSTER -> movie.setPosterUrl(url);
            case BACKDROP -> movie.setBackdropUrl(url);
            default -> {
            }
        }
        movieRepository.save(movie);
    }
}
