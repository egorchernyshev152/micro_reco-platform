package com.example.event.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        // описание публичного REST API для событий и статистики
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Event Service API")
                        .description("API for events and stats")
                        .version("1.0.0")
                        .contact(new Contact().name("Event Team")));
    }
}
