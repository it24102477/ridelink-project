package com.ridelink.account.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is served at /swagger-ui.html (also reachable at /swagger-ui/index.html)
 * and the raw OpenAPI JSON at /v3/api-docs. Both are permitted without auth in
 * SecurityConfig. This bean adds the "Authorize" (bearer JWT) button so protected
 * endpoints can be called directly from the UI: register/login on the left to get a
 * token, click Authorize and paste it in (no "Bearer " prefix needed), then try any
 * other endpoint.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI accountServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink - Account Service")
                        .description("Passenger/driver account registration, login & token issuance, "
                                + "role management, profile viewing/updating, account status management.")
                        .version("v1.0.0")
                        .contact(new Contact().name("RideLink").email("support@ridelink.example")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT returned by POST /api/accounts/login. "
                                        + "No \"Bearer \" prefix needed here.")));
    }
}
