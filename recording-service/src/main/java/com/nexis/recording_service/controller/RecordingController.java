package com.nexis.recording_service.controller;

import com.nexis.recording_service.dto.SessionRequestDto;
import com.nexis.recording_service.dto.SessionResponseDto;
import com.nexis.recording_service.service.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class RecordingController {

    private final RecordingService recordingService;

    @PostMapping("/start")
    public ResponseEntity<Void> startRecording(@RequestBody SessionRequestDto sessionRequestDto){
        recordingService.startRecording(sessionRequestDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Void> endRecording(@PathVariable UUID id){
        recordingService.endRecording(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDto> getSession(@PathVariable UUID id){
        return ResponseEntity.ok(recordingService.getRecording(id));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<StreamingResponseBody> getPlayback(@PathVariable UUID id) {

        recordingService.validate(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_NDJSON_VALUE)
                .body(outputStream -> {
                    recordingService.streamSessionEvents(id, outputStream);
                });
    }

}
