package com.nexis.recording_service.repository;

import com.nexis.recording_service.entity.SessionEventsEntity;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

@Repository
public interface SessionEventsRepository extends JpaRepository<SessionEventsEntity,Long> {

    @QueryHints(value = {
            @QueryHint(name = HINT_FETCH_SIZE, value = "100")
    })
    @Query("SELECT e FROM SessionEventsEntity e WHERE e.sessionId = :sessionId ORDER BY e.id ASC")
    Stream<SessionEventsEntity> streamAllBySessionIdOrderByIdAsc(@Param("sessionId") UUID sessionId);

}