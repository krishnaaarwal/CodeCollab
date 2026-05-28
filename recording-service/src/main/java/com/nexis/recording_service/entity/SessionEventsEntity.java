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

    /*
     * THE MAGIC HAPPENS HERE:
     * Tells Hibernate to map this Java object directly to a PostgreSQL JSONB column.
     * You can map this to an explicit DTO, a Map<String, Object>, or Jackson's JsonNode.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Object payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;
}