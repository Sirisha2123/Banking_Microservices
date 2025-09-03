package com.banking.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll() // Allow all for testing; adjust for production
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable) // Disable CSRF for simplicity
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable); // Disable basic auth for now

        return http.build();
    }
}