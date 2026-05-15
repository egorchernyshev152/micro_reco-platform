package com.example.catalog.controller;

import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.MoviePageResponse;
import com.example.catalog.dto.MovieSearchRequest;
import com.example.catalog.exception.GlobalExceptionHandler;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    @Mock
    private MovieService movieService;

    @InjectMocks
    private MovieController movieController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(movieController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createShouldReturn201AndPayload() throws Exception {
        MovieDto request = MovieDto.builder()
                .title("Inception")
                .description("Dream heist")
                .build();
        MovieDto response = MovieDto.builder()
                .id(5L)
                .title("Inception")
                .description("Dream heist")
                .averageRating(9.1)
                .build();
        when(movieService.create(any(MovieDto.class))).thenReturn(response);

        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Inception"));

        ArgumentCaptor<MovieDto> captor = ArgumentCaptor.forClass(MovieDto.class);
        verify(movieService).create(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Inception");
    }

    @Test
    void createShouldFailValidationWhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message", containsString("title")));

        verifyNoInteractions(movieService);
    }

    @Test
    void getShouldReturnNotFoundWhenServiceThrows() throws Exception {
        when(movieService.get(42L)).thenThrow(new NotFoundException("Movie not found: 42"));

        mockMvc.perform(get("/movies/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Movie not found: 42"));
    }

    @Test
    void deleteShouldMapDataIntegrityViolationTo409() throws Exception {
        doThrow(new DataIntegrityViolationException("fk")).when(movieService).delete(10L);

        mockMvc.perform(delete("/movies/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Constraint violation"));
    }

    @Test
    void getAllShouldReturn500WhenUnhandledError() throws Exception {
        when(movieService.getAll()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/movies/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("boom"));
    }

    @Test
    void findByIdsShouldPassParamsToService() throws Exception {
        when(movieService.findByIds(any())).thenReturn(List.of(
                MovieDto.builder().id(1L).title("A").build()
        ));

        mockMvc.perform(get("/movies/lookup").param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(movieService).findByIds(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void searchShouldBindModelAttribute() throws Exception {
        MoviePageResponse response = MoviePageResponse.builder()
                .items(List.of(MovieDto.builder().id(3L).title("Matrix").build()))
                .page(0)
                .size(5)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .build();
        when(movieService.search(any(MovieSearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/movies")
                        .param("genres", "sci-fi")
                        .param("ratingFrom", "8")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(movieService).search(any(MovieSearchRequest.class));
    }
}
