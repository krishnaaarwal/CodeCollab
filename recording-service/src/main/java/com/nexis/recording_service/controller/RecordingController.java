package com.nexis.recording_service.controller;

import com.nexis.recording_service.config.RabbitMqConfig;
import com.nexis.recording_service.dto.SessionRequestDto;
import com.nexis.recording_service.dto.SessionResponseDto;
import com.nexis.recording_service.entity.SessionEventsEntity;
import com.nexis.recording_service.repository.SessionRepository;
import com.nexis.recording_service.service.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class RecordingController {

    private final RecordingService recordingService;
    private final SessionRepository sessionRepository;

    @PostMapping("/start")
    public ResponseEntity<Void> startRecording(@RequestBody SessionRequestDto sessionRequestDto){
        return ResponseEntity.ok(recordingService.startRecording(sessionRequestDto));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Void> endRecording(@PathVariable UUID id){
        return ResponseEntity.ok(recordingService.endRecording(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDto> getSession(@PathVariable UUID id){
        return ResponseEntity.ok(recordingService.getRecording(id));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<StreamingResponseBody> getPlayback(@PathVariable UUID id) {
        if (!sessionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_NDJSON_VALUE)
                .body(outputStream -> {
                    recordingService.streamSessionEvents(id, outputStream);
                });
    }

}
