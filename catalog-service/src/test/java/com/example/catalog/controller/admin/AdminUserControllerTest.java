package com.example.catalog.controller.admin;

import com.example.catalog.dto.PageResponse;
import com.example.catalog.dto.admin.AdminUserComplaintsResponse;
import com.example.catalog.dto.admin.AdminUserResponse;
import com.example.catalog.dto.admin.UserAuditLogResponse;
import com.example.catalog.entity.AdminAuditAction;
import com.example.catalog.entity.UserRole;
import com.example.catalog.security.JwtAuthenticationFilter;
import com.example.catalog.security.RestAuthenticationEntryPoint;
import com.example.catalog.service.admin.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users возвращает пагинированный список")
    void shouldReturnPagedUsers() throws Exception {
        PageResponse<AdminUserResponse> page = PageResponse.<AdminUserResponse>builder()
                .items(List.of(AdminUserResponse.builder()
                        .id(1L)
                        .name("Demo Admin")
                        .email("admin@example.com")
                        .role(UserRole.ADMIN)
                        .blocked(false)
                        .complaintsCount(0)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .build();
        Mockito.when(adminUserService.search(eq("demo"), eq("ADMIN"), eq(false), eq(0), eq(20), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .param("query", "demo")
                        .param("role", "ADMIN")
                        .param("blocked", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].email").value("admin@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/users/{id}/block обновляет статус блокировки")
    void shouldUpdateBlockStatus() throws Exception {
        AdminUserResponse response = AdminUserResponse.builder()
                .id(10L)
                .name("User")
                .email("user@mail.com")
                .role(UserRole.USER)
                .blocked(true)
                .complaintsCount(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Mockito.when(adminUserService.updateBlockStatus(eq(10L), eq(true), eq("Спам"), Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/admin/users/10/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BlockRequest(true, "Спам"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/users/{id}/role меняет роль пользователя")
    void shouldUpdateRole() throws Exception {
        AdminUserResponse response = AdminUserResponse.builder()
                .id(11L)
                .name("User")
                .email("user@mail.com")
                .role(UserRole.ADMIN)
                .blocked(false)
                .complaintsCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Mockito.when(adminUserService.updateRole(eq(11L), eq(UserRole.ADMIN), eq("Повышение"), Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/admin/users/11/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleRequest(UserRole.ADMIN, "Повышение"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/users/{id} удаляет пользователя")
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/12"))
                .andExpect(status().isNoContent());

        Mockito.verify(adminUserService).deleteUser(eq(12L), Mockito.any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users/{id}/complaints возвращает жалобы")
    void shouldReturnComplaints() throws Exception {
        AdminUserComplaintsResponse response = AdminUserComplaintsResponse.builder()
                .openCount(2)
                .reviewingCount(1)
                .resolvedCount(1)
                .complaints(List.of())
                .build();
        Mockito.when(adminUserService.getComplaints(15L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/users/15/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users/{id}/audit возвращает аудит-лог")
    void shouldReturnAuditLog() throws Exception {
        Mockito.when(adminUserService.getAuditLog(20L)).thenReturn(List.of(
                UserAuditLogResponse.builder()
                        .id(1L)
                        .action(AdminAuditAction.BLOCK_UPDATED)
                        .details("Заблокирован")
                        .createdAt(Instant.now())
                        .build()
        ));

        mockMvc.perform(get("/api/admin/users/20/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private record BlockRequest(Boolean blocked, String reason) {
    }

    private record RoleRequest(UserRole role, String reason) {
    }
}
