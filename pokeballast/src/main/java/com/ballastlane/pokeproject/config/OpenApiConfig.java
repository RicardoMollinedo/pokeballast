package com.ballastlane.pokeproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html, with the bearer scheme and the optional audit headers. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI pokeBallastOpenApi() {
        return new OpenAPI()
                .info(new Info().title("PokeBallast API").version("v1")
                        .description("""
                                PokeAPI integration, local replication and editing of pokemon.
                                Every response is a BallastResponse: responseCode "0000" + response on success, \
                                an error code + errorDetail otherwise."""))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    /** Documents the audit headers on every operation once, instead of repeating them on each method. */
    @Bean
    OperationCustomizer auditHeadersCustomizer() {
        return (operation, handlerMethod) -> operation
                .addParametersItem(header("X-Forwarded-For", "Original client IP (audit)", new StringSchema()))
                .addParametersItem(header("x-user", "User invoking the endpoint (audit only, never used for authorization)",
                        new StringSchema()))
                .addParametersItem(header("x-latitude", "Client latitude, -90..90 (audit)", new NumberSchema()))
                .addParametersItem(header("x-longitude", "Client longitude, -180..180 (audit)", new NumberSchema()))
                .addParametersItem(header("x-channel", "Calling channel, e.g. WEB or MOBILE (audit)", new StringSchema()));
    }

    private static HeaderParameter header(String name, String description,
                                          io.swagger.v3.oas.models.media.Schema<?> schema) {
        HeaderParameter parameter = new HeaderParameter();
        parameter.name(name).description(description).required(false).schema(schema);
        return parameter;
    }
}
