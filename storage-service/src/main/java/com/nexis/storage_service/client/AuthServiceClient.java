package com.nexis.storage_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

// Tells Spring to route calls to the Auth Service via the API Gateway or direct service name
@FeignClient(name = "auth-service", url = "${nexis.auth-service.url:http://localhost:8081}")
public interface AuthServiceClient {

    @GetMapping("/api/auth/workspaces/{workspaceId}/check-member")
    boolean isWorkspaceMember(
            @PathVariable("workspaceId") UUID workspaceId,
            @RequestParam("userId") UUID userId
    );
}