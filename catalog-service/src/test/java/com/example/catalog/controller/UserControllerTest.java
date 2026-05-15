package com.example.catalog.controller;

import com.example.catalog.dto.UserDto;
import com.example.catalog.dto.UserUpsertRequest;
import com.example.catalog.entity.UserRole;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.GlobalExceptionHandler;
import com.example.catalog.service.UserService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createShouldReturnCreatedUser() throws Exception {
        UserUpsertRequest payload = new UserUpsertRequest();
        payload.setName("Alice");
        payload.setEmail("alice@mail.com");
        payload.setPassword("Secret123");
        payload.setRole(UserRole.USER);
        when(userService.create(any(UserUpsertRequest.class))).thenReturn(
                UserDto.builder().id(1L).name("Alice").email("alice@mail.com").build()
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void createShouldReturnConflictWhenServiceThrows() throws Exception {
        when(userService.create(any(UserUpsertRequest.class))).thenThrow(new ConflictException("Email already used"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new UserUpsertRequest() {{
                            setName("Bob");
                            setEmail("bob@mail.com");
                            setPassword("Secret123");
                            setRole(UserRole.USER);
                        }})))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already used"));
    }

    @Test
    void getAllShouldReturnUsers() throws Exception {
        when(userService.getAll()).thenReturn(List.of(
                UserDto.builder().id(1L).name("A").email("a@mail.com").build(),
                UserDto.builder().id(2L).name("B").email("b@mail.com").build()
        ));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(2));
    }
}
