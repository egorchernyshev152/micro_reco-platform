package com.example.catalog.service;

import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.MoviePageResponse;
import com.example.catalog.dto.MovieSearchRequest;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.MovieCastMember;
import com.example.catalog.entity.MovieStatus;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.MovieMapper;
import com.example.catalog.model.CastMemberModel;
import com.example.catalog.model.MovieModel;
import com.example.catalog.repository.MovieRepository;
import com.example.catalog.repository.spec.MovieSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    public MovieDto create(MovieDto dto) {
        MovieModel model = MovieMapper.fromDto(dto);
        Movie entity = MovieMapper.toEntity(model);
        if (entity.getStatus() == null) {
            entity.setStatus(MovieStatus.DRAFT);
        }
        Movie saved = movieRepository.save(entity);
        return MovieMapper.toDto(MovieMapper.toModel(saved));
    }

    public MovieDto update(Long id, MovieDto dto) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Movie not found: " + id));
        MovieModel model = MovieMapper.fromDto(dto);
        apply(movie, model);
        Movie saved = movieRepository.save(movie);
        return MovieMapper.toDto(MovieMapper.toModel(saved));
    }

    public MovieDto get(Long id) {
        return movieRepository.findById(id)
                .map(MovieMapper::toModel)
                .map(MovieMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Movie not found: " + id));
    }

    public List<MovieDto> getAll() {
        return movieRepository.findAll().stream()
                .map(MovieMapper::toModel)
                .map(MovieMapper::toDto)
                .toList();
    }

    public void delete(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new NotFoundException("Movie not found: " + id);
        }
        movieRepository.deleteById(id);
    }

    public List<MovieDto> findByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return movieRepository.findByIdIn(ids).stream()
                .map(MovieMapper::toModel)
                .map(MovieMapper::toDto)
                .toList();
    }

    public MoviePageResponse search(MovieSearchRequest filter) {
        var statuses = CollectionUtils.isEmpty(filter.getStatuses())
                ? EnumSet.of(MovieStatus.PUBLISHED)
                : filter.getStatuses();
        Specification<Movie> spec = Specification.where(MovieSpecifications.queryText(filter.getQuery()))
                .and(MovieSpecifications.withGenres(filter.getGenres()))
                .and(MovieSpecifications.withCountries(filter.getCountries()))
                .and(MovieSpecifications.withTags(filter.getTags()))
                .and(MovieSpecifications.withCast(filter.getCast()))
                .and(MovieSpecifications.releaseYearFrom(filter.getReleaseYearFrom()))
                .and(MovieSpecifications.releaseYearTo(filter.getReleaseYearTo()))
                .and(MovieSpecifications.durationFrom(filter.getDurationFrom()))
                .and(MovieSpecifications.durationTo(filter.getDurationTo()))
                .and(MovieSpecifications.ratingFrom(filter.getRatingFrom()))
                .and(MovieSpecifications.ratingTo(filter.getRatingTo()))
                .and(MovieSpecifications.withStatuses(statuses));
        Pageable pageable = PageRequest.of(resolvePage(filter.getPage()), resolveLimit(filter.getLimit()), resolveSort(filter.getSort()));
        Page<Movie> page = movieRepository.findAll(spec, pageable);
        List<MovieDto> items = page.stream()
                .map(MovieMapper::toModel)
                .map(MovieMapper::toDto)
                .toList();
        return MoviePageResponse.builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    private void apply(Movie movie, MovieModel model) {
        movie.setTitle(model.getTitle());
        movie.setOriginalTitle(model.getOriginalTitle());
        movie.setOriginalLanguage(model.getOriginalLanguage());
        movie.setDescription(model.getDescription());
        movie.setSynopsis(model.getSynopsis());
        movie.setReleaseYear(model.getReleaseYear());
        movie.setReleaseDate(model.getReleaseDate());
        movie.setDurationMinutes(model.getDurationMinutes());
        movie.setAgeRating(model.getAgeRating());
        movie.setTagline(model.getTagline());
        movie.setStatus(model.getStatus() != null ? model.getStatus() : MovieStatus.DRAFT);
        movie.setPosterUrl(model.getPosterUrl());
        movie.setBackdropUrl(model.getBackdropUrl());
        movie.setTrailerUrl(model.getTrailerUrl());
        movie.setBudget(model.getBudget());
        movie.setRevenue(model.getRevenue());
        movie.setGenres(new java.util.LinkedHashSet<>(model.getGenres()));
        movie.setCountries(new java.util.LinkedHashSet<>(model.getCountries()));
        movie.setTags(new java.util.LinkedHashSet<>(model.getTags()));
        movie.setCast(copyCast(model.getCast()));
    }

    private Sort resolveSort(String sortKey) {
        if (sortKey == null) {
            return Sort.by(Sort.Direction.DESC, "averageRating");
        }
        return switch (sortKey) {
            case "rating_asc" -> Sort.by(Sort.Direction.ASC, "averageRating");
            case "year_desc" -> Sort.by(Sort.Direction.DESC, "releaseYear");
            case "year_asc" -> Sort.by(Sort.Direction.ASC, "releaseYear");
            case "created_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "created_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "averageRating");
        };
    }

    private int resolveLimit(Integer limit) {
        int value = limit == null ? 20 : limit;
        return Math.max(1, Math.min(100, value));
    }

    private int resolvePage(Integer page) {
        int value = page == null ? 0 : page;
        return Math.max(0, value);
    }

    private List<MovieCastMember> copyCast(List<CastMemberModel> cast) {
        if (cast == null) {
            return new ArrayList<>();
        }
        return cast.stream()
                .map(model -> MovieCastMember.builder()
                        .personTmdbId(model.getTmdbId())
                        .name(model.getName())
                        .character(model.getCharacter())
                        .profileUrl(model.getProfileUrl())
                        .orderIndex(model.getOrderIndex())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
