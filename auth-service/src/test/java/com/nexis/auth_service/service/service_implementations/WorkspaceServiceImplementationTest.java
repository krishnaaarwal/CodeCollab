package com.nexis.auth_service.service.service_implementations;

import com.nexis.auth_service.config.type.WorkspaceRole;
import com.nexis.auth_service.dto.workspace.WorkspaceRequestDto;
import com.nexis.auth_service.dto.workspace.WorkspaceResponseDto;
import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.entity.WorkspaceEntity;
import com.nexis.auth_service.entity.WorkspaceMemberEntity;
import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.repository.WorkspaceRepository;
import com.nexis.auth_service.security.user_principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any; // FIXED IMPORT
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("Workspace service test")
class WorkspaceServiceImplementationTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceServiceImplementation workspaceServiceImplementation;

    private WorkspaceRequestDto workspaceRequestDto;
    private WorkspaceEntity workspaceEntity;
    private UserEntity userEntity;
    private UUID fakeUserId;

    @BeforeEach
    void setup(){
        // Prepping our ingredients before the test starts
        fakeUserId = UUID.randomUUID();

        this.userEntity = UserEntity.builder()
                .id(fakeUserId)
                .email("myname@test.com")
                .fullname("WATASHI NO NAMAE WA KAMI DESU")
                .build();

        this.workspaceRequestDto = WorkspaceRequestDto.builder()
                .name("Test Workspace")
                .description("A cool workspace")
                .visibility("PUBLIC")
                .build();

        this.workspaceEntity = WorkspaceEntity.builder()
                .id(UUID.randomUUID())
                .name("Test Workspace")
                .ownerId(fakeUserId)
                .build();
    }

    @Nested
    @DisplayName("Create Workspace Test")
    class CreateWorkspaceTest{

        @Test
        @DisplayName("Should create workspace successfully")
        void shouldCreateWorkspaceSuccessfully(){

            // --- 1. GIVEN (Rigging the props) ---

            // A. Rig the Security Context (Fake a logged-in user)
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            UserPrincipal fakePrincipal = new UserPrincipal(userEntity);
            when(authentication.getPrincipal()).thenReturn(fakePrincipal);

            // B. Rig the Databases
            when(userRepository.findById(fakeUserId)).thenReturn(Optional.of(userEntity));
            when(workspaceRepository.save(any(WorkspaceEntity.class))).thenReturn(workspaceEntity);

            // --- 2. WHEN (The Chef cooks) ---
            WorkspaceResponseDto result = workspaceServiceImplementation.createWorkspace(workspaceRequestDto);

            // --- 3. THEN (Taste the food) ---
            assertNotNull(result);
            assertEquals("Test Workspace", result.getName());

            verify(workspaceRepository,times(1)).save(workspaceEntity);
            verify(workspaceRepository.save(argThat(workspaceEntity->workspaceEntity.getName().equals("Test Workspace"))));
        }
    }
}