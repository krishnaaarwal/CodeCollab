package com.nexis.recording_service.repository;

import com.nexis.recording_service.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    // Spring Data JPA automatically writes the SQL for this based on the method name
    List<SessionEntity> findByWorkspaceIdOrderByStartedAtDesc(UUID workspaceId);
}