package com.nexis.recording_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionEventsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Object payload;/*
     * Tells Hibernate to map this Java object directly to a PostgreSQL JSONB column.
     * You can map this to an explicit DTO, a Map<String, Object>, or Jackson's JsonNode.
     */

    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;


    public SessionEventsEntity(UUID sessionId, String eventType, UUID userId, Object payload, LocalDateTime timestamp) {
        this.id = null; // Left null explicitly so Postgres allocates the IDENTITY value on insert
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.userId = userId;
        this.payload = payload; // Holds the full CodeOperation or ChatMessage object for JSONB
        this.timestamp = timestamp;
    }
}