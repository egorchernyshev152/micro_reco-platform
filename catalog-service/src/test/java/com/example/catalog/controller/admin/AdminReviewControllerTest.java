package com.example.catalog.controller.admin;

import com.example.catalog.dto.admin.AdminReviewBulkActionRequest;
import com.example.catalog.dto.admin.AdminReviewPageResponse;
import com.example.catalog.dto.admin.AdminReviewResponse;
import com.example.catalog.dto.admin.AdminReviewStatsResponse;
import com.example.catalog.entity.ReviewStatus;
import com.example.catalog.security.JwtAuthenticationFilter;
import com.example.catalog.security.RestAuthenticationEntryPoint;
import com.example.catalog.service.admin.AdminReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminReviewService adminReviewService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/reviews возвращает список с метриками")
    void shouldReturnReviewsWithStats() throws Exception {
        AdminReviewResponse review = AdminReviewResponse.builder()
                .id(1L)
                .movieId(2L)
                .movieTitle("Movie")
                .userId(3L)
                .userName("Author")
                .userEmail("author@mail.com")
                .score(9)
                .content("text")
                .status(ReviewStatus.PUBLISHED)
                .flagged(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        AdminReviewPageResponse response = AdminReviewPageResponse.builder()
                .items(List.of(review))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .stats(AdminReviewStatsResponse.builder()
                        .total(1)
                        .pending(0)
                        .published(1)
                        .spam(0)
                        .deleted(0)
                        .flagged(0)
                        .build())
                .build();
        Mockito.when(adminReviewService.search(eq("movie"), eq(2L), eq(3L), eq(ReviewStatus.PUBLISHED), eq(true), eq(0), eq(20), eq("createdAt,desc")))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("query", "movie")
                        .param("movieId", "2")
                        .param("userId", "3")
                        .param("status", "PUBLISHED")
                        .param("flagged", "true")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.stats.published").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/reviews/{id}/status обновляет статус")
    void shouldUpdateReviewStatus() throws Exception {
        AdminReviewResponse response = AdminReviewResponse.builder()
                .id(10L)
                .movieId(1L)
                .movieTitle("Movie")
                .userId(5L)
                .userName("Critic")
                .userEmail("critic@mail.com")
                .score(5)
                .content("text")
                .status(ReviewStatus.SPAM)
                .flagged(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Mockito.when(adminReviewService.updateStatus(eq(10L), eq(ReviewStatus.SPAM), eq("Спам"), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/admin/reviews/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusRequest("SPAM", "Спам"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SPAM"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/reviews/bulk-action вызывает сервис")
    void shouldRunBulkAction() throws Exception {
        mockMvc.perform(post("/api/admin/reviews/bulk-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkRequest(new Long[]{1L, 2L}, "DELETED", "Причина"))))
                .andExpect(status().isNoContent());

        ArgumentCaptor<AdminReviewBulkActionRequest> captor = ArgumentCaptor.forClass(AdminReviewBulkActionRequest.class);
        Mockito.verify(adminReviewService).bulkAction(captor.capture(), any());
        assertThat(captor.getValue().getIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.DELETED);
    }

    private record StatusRequest(String status, String reason) {
    }

    private record BulkRequest(Long[] ids, String status, String reason) {
    }
}
