package com.nexis.recording_service.controller;

import com.nexis.recording_service.dto.SessionRequestDto;
import com.nexis.recording_service.dto.SessionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class RecordingController {

    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping("/start")
    public ResponseEntity<Void> startRecording(@RequestBody SessionRequestDto sessionRequestDto){
       //THIS WILL GO TO SERVICE LAYER LATER, JUST SHOWING HOW I WILL DO IT!
        UUID sessionId = UUID.randomUUID();
        redisTemplate.opsForValue().set("nexis:active-session:"+sessionRequestDto.workspaceId(),sessionId.toString(),Duration.ofHours(16));
        return null;
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Void> endRecording(@PathVariable UUID id){
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDto> getSession(@PathVariable UUID id){
        return null;
    }

    // SKELETAL ARCHITECTURE: NOT FOR COPY-PASTE PRODUCTION USE
    @GetMapping("/{id}/events")
    public ResponseEntity<StreamingResponseBody> getPlayback(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_NDJSON_VALUE) // Newline Delimited JSON
                .body(outputStream -> {
                    // 1. Open a low-level, scrolling database cursor (Stream<SessionEventsEntity>)
                    // 2. Loop through rows one-by-one
                    // 3. Convert single row to JSON bytes using ObjectMapper
                    // 4. outputStream.write(bytes); outputStream.flush();
                    // 5. Automatically free memory per row
                });
    }

}
