package com.bschool.msgrestapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        final String bearerAuth = "bearerAuth";
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(bearerAuth, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenu via POST /api/auth/login ou /api/auth/register")));
    }
}
