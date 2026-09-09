package com.videoai.video_intelligence.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration.
 * Once the application starts, documentation is available at:
 *   - Swagger UI:  http://localhost:8081/swagger-ui.html
 *   - OpenAPI JSON: http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI videoIntelligenceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Video Intelligence Search API")
                        .description("A B2B API that makes video libraries fully searchable and shoppable. "
                                + "Upload videos, extract audio, transcribe with Google Cloud Speech-to-Text, "
                                + "generate Vertex AI embeddings, and perform sub-300ms semantic search "
                                + "using pgvector to find the exact moment in a video.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Video Intelligence Team")
                                .email("contact@videoai.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Development Server")
                ));
    }
}
