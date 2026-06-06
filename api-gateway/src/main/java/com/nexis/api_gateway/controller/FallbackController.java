package com.nexis.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        return buildFallbackResponse("Auth & Workspace Service is currently down. Please try again later.", "auth-service");
    }

    @RequestMapping("/execute")
    public Mono<ResponseEntity<Map<String, Object>>> executionServiceFallback() {
        return buildFallbackResponse("Code Execution Service is busy or down. Compilations are temporarily unavailable.", "execution-service");
    }

    @RequestMapping("/storage")
    public Mono<ResponseEntity<Map<String, Object>>> storageServiceFallback() {
        return buildFallbackResponse("File Storage Service failed to respond. Your project tree cannot be loaded right now.", "storage-service");
    }

    @RequestMapping("/websocket")
    public Mono<ResponseEntity<Map<String, Object>>> webSocketServiceFallback() {
        return buildFallbackResponse("Collaboration signaling server is down. Real-time editing is offline.", "websocket-service");
    }

    @RequestMapping("/record")
    public Mono<ResponseEntity<Map<String, Object>>> recordingServiceFallback() {
        return buildFallbackResponse("Session Recording Service is currently unavailable.", "recording-service");
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(String message, String serviceName) {
        Map<String, Object> response = Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "message", message,
                "timestamp", LocalDateTime.now().toString(),
                "service", serviceName
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}