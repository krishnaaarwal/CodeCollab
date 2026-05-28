package com.nexis.recording_service.repository;

import com.nexis.recording_service.entity.SessionEventsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionEventsRepository extends JpaRepository<SessionEventsEntity,Long> {
}
