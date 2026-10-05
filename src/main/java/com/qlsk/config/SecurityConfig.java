package com.qlsk.config;

import com.qlsk.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationProvider authenticationProvider(
            com.qlsk.security.CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        org.springframework.security.authentication.dao.DaoAuthenticationProvider authProvider = 
                new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Static resources & frontend pages
                .requestMatchers("/", "/index.html", "/login.html", "/register.html", 
                                 "/event-detail.html", "/my-tickets.html", "/profile.html", "/registered-events.html",
                                 "/organizer/**", "/admin/**",
                                 "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico", "/error").permitAll()
                // Public auth and public event APIs
                .requestMatchers("/api/auth/**", "/api/upload/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/feedbacks/event/**").permitAll()
                
                // Admin specific APIs
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Organizer specific APIs (Admin also allowed)
                .requestMatchers("/api/organizer/**").hasAnyRole("ORGANIZER", "ADMIN")
                
                // Registered Attendee and common authenticated endpoints
                .requestMatchers("/api/registrations/**").authenticated()
                .requestMatchers("/api/users/profile").authenticated()
                .requestMatchers("/api/feedbacks/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
