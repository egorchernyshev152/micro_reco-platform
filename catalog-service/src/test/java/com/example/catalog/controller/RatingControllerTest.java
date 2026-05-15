package com.example.catalog.controller;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.exception.GlobalExceptionHandler;
import com.example.catalog.service.RatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

    @Mock
    private RatingService ratingService;

    @InjectMocks
    private RatingController ratingController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ratingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addShouldReturnCreatedRating() throws Exception {
        RatingDto payload = RatingDto.builder()
                .userId(1L)
                .movieId(2L)
                .score(8)
                .build();
        when(ratingService.add(any(RatingDto.class))).thenReturn(
                RatingDto.builder()
                        .id(10L)
                        .userId(1L)
                        .movieId(2L)
                        .score(8)
                        .createdAt(Instant.EPOCH)
                        .build()
        );

        mockMvc.perform(post("/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.score").value(8));
    }

    @Test
    void byUserShouldDelegateToService() throws Exception {
        when(ratingService.byUser(3L)).thenReturn(List.of(
                RatingDto.builder().id(1L).userId(3L).movieId(5L).score(5).build()
        ));

        mockMvc.perform(get("/ratings/by-user/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(3));

        verify(ratingService).byUser(3L);
    }

    @Test
    void byMovieShouldDelegateToService() throws Exception {
        when(ratingService.byMovie(9L)).thenReturn(List.of(
                RatingDto.builder().id(2L).userId(1L).movieId(9L).score(3).build()
        ));

        mockMvc.perform(get("/ratings/by-movie/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieId").value(9));

        verify(ratingService).byMovie(9L);
    }
}
