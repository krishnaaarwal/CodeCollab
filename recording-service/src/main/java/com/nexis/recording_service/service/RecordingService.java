package com.nexis.recording_service.service;

import com.nexis.recording_service.dto.SessionRequestDto;
import com.nexis.recording_service.dto.SessionResponseDto;
import com.nexis.recording_service.entity.SessionEntity;
import com.nexis.recording_service.entity.SessionEventsEntity;
import com.nexis.recording_service.repository.SessionEventsRepository;
import com.nexis.recording_service.repository.SessionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
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

    public void startRecording(SessionRequestDto sessionRequestDto) {
        String key = "nexis:active-session:" + sessionRequestDto.workspaceId();
        UUID newSessionId = UUID.randomUUID();

        Boolean isAbsent = redisTemplate.opsForValue()
                .setIfAbsent(key, newSessionId.toString(), Duration.ofHours(16));

        if (Boolean.FALSE.equals(isAbsent)) {
            throw new IllegalArgumentException("Workspace session is already actively recording.");
        }

        SessionEntity sessionEntity = SessionEntity.builder().id(newSessionId).workspaceId(sessionRequestDto.workspaceId()).startedAt(LocalDateTime.now()).participants(sessionRequestDto.participants()).build();
        sessionRepository.save(sessionEntity);
    }

    public void endRecording(UUID id) {
        SessionEntity sessionEntity = sessionRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Session not found with Id"+id));
        UUID workspaceId = sessionEntity.getWorkspaceId();

        redisTemplate.delete("nexis:active-session:" + workspaceId);

        sessionEntity.setEndedAt(LocalDateTime.now());
        sessionEntity.setDuration(Duration.between(sessionEntity.getStartedAt(),sessionEntity.getEndedAt()));
        sessionRepository.save(sessionEntity);
    }

    public SessionResponseDto getRecording(UUID id) {

        SessionEntity sessionEntity = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with Id: " + id));
        return new SessionResponseDto(sessionEntity);
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

    public void validate(UUID id) throws IllegalArgumentException{
        if(!sessionRepository.existsById(id)) throw new IllegalArgumentException("Session Id does not exist"+id);
    }
}
