package com.ridelink.ride.config;

import com.ridelink.ride.security.JwtAuthFilter;
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

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) { this.jwtAuthFilter = jwtAuthFilter; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                // AdminController
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // PassengerController
                .requestMatchers(HttpMethod.POST, "/api/rides/estimate").hasRole("PASSENGER")
                // DriverController
                .requestMatchers(HttpMethod.GET, "/api/rides/driver/*").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PUT, "/api/rides/*/*/accept", "/api/rides/*/start",
                        "/api/rides/*/complete", "/api/rides/*/cancel").hasRole("DRIVER")
                // UserController
                .requestMatchers(HttpMethod.GET, "/api/rides/users/*/open-rides", "/api/rides/*")
                        .hasAnyRole("PASSENGER", "DRIVER", "ADMIN")
                // PassengerController: /api/{userId}/rides and everything under it
                .requestMatchers("/api/*/rides", "/api/*/rides/**").hasRole("PASSENGER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
