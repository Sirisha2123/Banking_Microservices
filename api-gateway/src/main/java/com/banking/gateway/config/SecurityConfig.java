package com.banking.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Mono;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.skip-validation:false}")
    private boolean skipValidation;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        System.out.println("Configuring SecurityWebFilterChain with role-based security, skipValidation: " + skipValidation);

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ enable CORS
            .authorizeExchange(exchanges -> {
                exchanges
                    .pathMatchers("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                if (skipValidation) {
                    exchanges.anyExchange().permitAll(); // Bypass all authentication for testing
                } else {
                    exchanges
                        .pathMatchers("/customers/**", "/accounts/**", "/transactions/**", "/notifications/**", "/reports/transactions/**")
                        .hasAnyRole("CUSTOMER", "EMPLOYEE", "ADMIN")
                        .pathMatchers("/customers/{id}/approve-kyc", "/accounts/{id}/hold")
                        .hasAnyRole("EMPLOYEE", "ADMIN")
                        .pathMatchers("/audits", "/reports/audits")
                        .hasRole("ADMIN")
                        .anyExchange().authenticated();
                }
            })
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .exceptionHandling(exchanges -> exchanges.authenticationEntryPoint((exchange, ex) -> {
                System.out.println("Authentication failed for path: " +
                        exchange.getRequest().getPath().value() + " - " + ex.getMessage());
                return Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED));
            }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:4200")); // ✅ Angular frontend
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
