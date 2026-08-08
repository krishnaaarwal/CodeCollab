package com.nexis.storage_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@FeignClient(name = "auth-service", url = "http://auth-service:8081")
public interface AuthServiceClient {
    @GetMapping("/api/auth/internal/workspaces/{workspaceId}/check-member")
    boolean isWorkspaceMember(
            @PathVariable("workspaceId") UUID workspaceId,
            @RequestParam("userId") UUID userId
    );
}