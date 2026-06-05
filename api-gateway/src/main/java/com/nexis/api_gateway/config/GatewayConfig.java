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
import reactor.core.publisher.Mono;

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
                // ROUTE A: PUBLIC AUTH GATEWAY (No JWT checking required)
                .route("auth-public-route", r -> r
                        .path("/api/auth/login", "/api/auth/signup", "/api/auth/refresh", "/api/auth/oauth/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c.setRateLimiter(customRedisRateLimiter()).setKeyResolver(ipKeyResolver()))
                                .circuitBreaker(c -> c.setName("auth-public").setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri("lb://auth-service")
                )
                // ROUTE B: SECURE ACCOUNT/WORKSPACE OPERATIONS (Protected by Filter)
                .route("auth-protected-route", r -> r
                        .path("/api/auth/me", "/api/workspaces/**")
                        .filters(f -> f

                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))

                                .requestRateLimiter(c -> c.setRateLimiter(customRedisRateLimiter()).setKeyResolver(userIdKeyResolver()))
                                .circuitBreaker(c -> c.setName("auth-protected").setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri("lb://auth-service")
                )
                .build();
    }

}
