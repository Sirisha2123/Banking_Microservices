package com.banking.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableWebFlux
@EnableJpaRepositories(basePackages = "com.banking.auth.repository")
public class WebFluxConfig {

    @Bean
    public Flux<?> initializeRepository() {
        // This ensures JPA repositories are initialized in a reactive context
        return Flux.defer(() -> Flux.empty())
                .subscribeOn(Schedulers.boundedElastic());
    }
}