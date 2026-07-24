package com.nexis.recording_service.controller;

import com.nexis.recording_service.dto.SessionRequestDto;
import com.nexis.recording_service.dto.SessionResponseDto;
import com.nexis.recording_service.service.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class RecordingController {

    private final RecordingService recordingService;

    @PostMapping("/start")
    public ResponseEntity<SessionResponseDto> startRecording(@RequestBody SessionRequestDto request) {
        SessionResponseDto response = recordingService.startRecording(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    // In RecordingController.java
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<SessionResponseDto>> getWorkspaceSessions(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(recordingService.getSessionsByWorkspace(workspaceId));
    }

}
