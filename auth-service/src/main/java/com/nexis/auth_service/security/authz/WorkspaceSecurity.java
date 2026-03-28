package com.nexis.auth_service.security.authz;

import com.nexis.auth_service.config.type.WorkspaceRole;
import com.nexis.auth_service.repository.WorkspaceMemberRepository;
import com.nexis.auth_service.security.user_principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("workspaceSecurity")
@RequiredArgsConstructor
public class WorkspaceSecurity {
    private final WorkspaceMemberRepository memberRepository;

    public boolean isOwner(UUID workspaceId) {
        UUID userId = getCurrentUserId();

        return memberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER);
    }

    public boolean isOwnerOrAdmin(UUID workspaceId) {
        UUID userId = getCurrentUserId();
        return memberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER) ||
                memberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.ADMIN);
    }

    public boolean isMember(UUID workspaceId) {
        UUID userId = getCurrentUserId();
        return memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    // Helper method to grab the user from the JWT Security Context
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null; // Not logged in
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getUserEntity().getId();
    }


}
