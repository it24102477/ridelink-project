package com.ridelink.fare.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fare.exception.AccessDeniedMessages;
import com.ridelink.fare.exception.ApiError;
import com.ridelink.fare.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                // /estimate is used by prospective passengers before a ride exists; /final and
                // POST /payments are called server-to-server by ride-management-service when it
                // completes a ride (RideService never forwards the caller's JWT downstream, so
                // gating these behind .authenticated() would make ride completion fail with 401
                // every time - this mirrors the existing /api/fares/final justification).
                .requestMatchers(HttpMethod.POST,
                        "/api/fares/estimate", "/api/fares/estimate-coordinates", "/api/fares/final", "/api/payments").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                // AdminController
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // PassengerController
                .requestMatchers("/api/passenger/**").hasRole("PASSENGER")
                // DriverController: driver-facing, called with the driver's own bearer token.
                // RideCompletionService then double-checks the caller's driverId and the ride's
                // IN_PROGRESS status before it does anything.
                .requestMatchers("/api/driver/**").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/payments/rides/*/*/Payment").hasRole("DRIVER")
                // UserController
                .requestMatchers(HttpMethod.GET, "/api/payments/*", "/api/payments/*/receipt")
                        .hasAnyRole("PASSENGER", "DRIVER", "ADMIN")
                .anyRequest().authenticated()
            )
            // Role failures (e.g. a PASSENGER token on the driver payment API) and missing/invalid
            // tokens are rejected by the filter chain before any controller runs, so they need their
            // own JSON body - otherwise the client would get an empty response.
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> writeError(res, HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> writeError(res, HttpStatus.FORBIDDEN)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse res, HttpStatus status) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(),
                new ApiError(status.value(), status.getReasonPhrase(), AccessDeniedMessages.NOT_PERMITTED, null));
    }
}
