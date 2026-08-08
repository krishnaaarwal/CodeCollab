package com.nexis.api_gateway.config;

import com.nexis.api_gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public GatewayConfig(JwtAuthenticationFilter jwtAuthenticationFilter){
        this.jwtAuthenticationFilter=jwtAuthenticationFilter;
    }

    // RESOLVER A: Fallback/Public Route Key Resolver (IP Address Based)
    @Bean
    @Primary // Tells Spring to use this as the default if a generic reference is made
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            String ip = "unknown";
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                ip = remoteAddress.getAddress().getHostAddress();
            }
            return Mono.just(ip);
        };
    }

    // RESOLVER B: Protected Route Key Resolver (User ID Based)
    @Bean
    public KeyResolver userIdKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

            if (userId == null || userId.isBlank()) {
                return Mono.just("anonymous-rate-limit-bucket");
            }

            return Mono.just(userId);
        };
    }

    @Bean
    public RedisRateLimiter customRedisRateLimiter() {
        return new RedisRateLimiter(100, 200);
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // =========================================================================
                // 1. AUTH SERVICE DOMAIN (Port 8081)
                // =========================================================================
                // ROUTE A: PUBLIC AUTH & OAUTH GATEWAY (IP-Based Rate Limiting)
                .route("auth-public-route", r -> r
                        .path("/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/oauth2/**",
                                "/api/login/oauth2/code/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c.setRateLimiter(customRedisRateLimiter()).setKeyResolver(ipKeyResolver()))
                                .circuitBreaker(c -> c.setName("auth-public").setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri("http://auth-service:8081")
                )

                // ROUTE B: PROTECTED AUTH & WORKSPACE MANAGEMENT
                .route("auth-protected-route", r -> r
                        .path("/api/auth/me",
                                "/api/auth/logout",
                                "/api/workspaces/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c.setRateLimiter(customRedisRateLimiter()).setKeyResolver(userIdKeyResolver()))
                                .circuitBreaker(c -> c.setName("auth-protected").setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri("http://auth-service:8081")
                )

                // =========================================================================
                // 2. EXECUTION SERVICE DOMAIN (Port 8083)
                // =========================================================================
                .route("execution-service-route", r -> r
                        .path("/api/execute/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c.setRateLimiter(customRedisRateLimiter()).setKeyResolver(userIdKeyResolver()))
                                .circuitBreaker(c -> c.setName("execution-service").setFallbackUri("forward:/fallback/execute"))
                        )
                        .uri("http://execution-service:8083")
                )

                // =========================================================================
                // 3. STORAGE SERVICE DOMAIN (Port 8084)
                // =========================================================================
                .route("storage-service-route", r -> r
                        .path("/api/files/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c.setRateLimiter(customRedisRateLimiter()).setKeyResolver(userIdKeyResolver()))
                                .circuitBreaker(c -> c.setName("storage-service").setFallbackUri("forward:/fallback/storage"))
                        )
                        .uri("http://storage-service:8084")
                )

                // =========================================================================
                // 4. RECORDING SERVICE DOMAIN (Port 8085)
                // =========================================================================
                .route("recording-service-route", r -> r
                        .path("/api/sessions/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> c.setRateLimiter(customRedisRateLimiter()).setKeyResolver(userIdKeyResolver()))
                                .circuitBreaker(c -> c.setName("recording-service").setFallbackUri("forward:/fallback/recording"))
                        )
                        .uri("http://recording-service:8085")
                )
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        corsConfig.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://nexis.local"
        ));

        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-User-Id"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
