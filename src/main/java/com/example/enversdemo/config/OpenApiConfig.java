package com.example.enversdemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI enversDemoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Envers Demo API")
                        .version("1.0.0")
                        .description("CRUD + Hibernate Envers audit history for a Product entity, "
                                + "running on Hibernate ORM 7.4.1.Final."));
    }
}
