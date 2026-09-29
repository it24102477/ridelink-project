package com.ridelink.driver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.exception.ApiError;
import com.ridelink.driver.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()

                // AdminController
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // UserController: deleting a profile by account id is for DRIVER (own) and ADMIN only
                .requestMatchers(HttpMethod.DELETE, "/api/drivers/user/*").hasAnyRole("DRIVER", "ADMIN")
                // UserController
                .requestMatchers("/api/drivers/**").hasAnyRole("PASSENGER", "DRIVER", "ADMIN")

                // DriverController: /api/{userId}/drivers and everything under it
                .requestMatchers("/api/*/drivers", "/api/*/drivers/**").hasRole("DRIVER")

                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> writeError(res, HttpStatus.UNAUTHORIZED,
                        "Authentication is required. Send a valid Bearer token."))
                .accessDeniedHandler((req, res, e) -> writeError(res, HttpStatus.FORBIDDEN,
                        "You do not have permission to access this resource")))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse res, HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(),
                new ApiError(status.value(), status.getReasonPhrase(), message, null));
    }
}