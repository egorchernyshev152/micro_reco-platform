package com.example.catalog.dto.importer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbImportRequest {
    @Min(1)
    @Max(100)
    private int pages = 1;

    private String language = "en-US";

    /**
     * ISO-код оригинального языка (например, "en").
     */
    private String originalLanguage = "en";

    /**
     * ISO-код страны производства (например, "US"). Используется для обратной совместимости,
     * рекомендуется передавать массив `originCountries`.
     */
    private String originCountry = "US";

    /**
     * Набор стран производства. Если не задан, используется {@link #originCountry}.
     */
    private Set<String> originCountries = new LinkedHashSet<>();

    @Min(1900)
    private Integer yearFrom = 1990;

    @Min(1900)
    private Integer yearTo = 2026;

    @Min(0)
    private Integer minVoteCount = 50;

    @DecimalMin(value = "0.0")
    private Double minVoteAverage = 5.0;

    private boolean includeAdult = false;

    /**
     * Необязательный набор TMDb-идентификаторов жанров (например, 28 — боевик).
     */
    private Set<Integer> genreIds = new LinkedHashSet<>();

    public Set<String> resolveOriginCountries() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(originCountries)) {
            originCountries.stream()
                    .filter(StringUtils::hasText)
                    .map(code -> code.trim().toUpperCase())
                    .forEach(result::add);
        }
        if (StringUtils.hasText(originCountry)) {
            Arrays.stream(originCountry.split("[,; ]+"))
                    .filter(StringUtils::hasText)
                    .map(code -> code.trim().toUpperCase())
                    .forEach(result::add);
        }
        if (result.isEmpty()) {
            result.add("US");
        }
        return result;
    }
}
