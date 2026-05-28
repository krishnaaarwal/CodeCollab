package com.nexis.recording_service.service;

import com.nexis.recording_service.dto.SessionRequestDto;
import com.nexis.recording_service.dto.SessionResponseDto;
import com.nexis.recording_service.entity.SessionEventsEntity;
import com.nexis.recording_service.repository.SessionEventsRepository;
import com.nexis.recording_service.repository.SessionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SessionEventsRepository sessionEventsRepository;
    private final SessionRepository sessionRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public Void startRecording(SessionRequestDto sessionRequestDto) {
        UUID sessionId = UUID.randomUUID();
        redisTemplate.opsForValue().set("nexis:active-session:"+sessionRequestDto.workspaceId(),sessionId.toString(), Duration.ofHours(16));
        return null;
    }

    public Void endRecording(UUID id) {
    }

    public SessionResponseDto getRecording(UUID id) {
    }


        @Transactional(readOnly = true) // Crucial for database cursors
        public void streamSessionEvents(UUID sessionId, java.io.OutputStream outputStream) {
            // 1. Invoke the streaming repository method inside a try-with-resources block
            try (Stream<SessionEventsEntity> eventStream = sessionEventsRepository.streamAllBySessionIdOrderByTimestampAsc(sessionId)) {

                eventStream.forEach(event -> {
                    try {
                        // 2. Convert single entity to JSON line bytes and flush to network card
                        byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
                        outputStream.write(jsonBytes);
                        outputStream.write('\n'); // Newline delimited
                        outputStream.flush();

                        // 3. PHYSICAL MEMORY DETACHMENT: Evict entity from Hibernate L1 cache immediately
                        entityManager.detach(event);

                    } catch (Exception e) {
                        throw new RuntimeException("Network write failure during stream playback", e);
                    }
                });
            }
        }

}
