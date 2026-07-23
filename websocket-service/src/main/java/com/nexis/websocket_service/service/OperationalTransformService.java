package com.nexis.websocket_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexis.websocket_service.exception.WorkspaceBusyException;
import com.nexis.websocket_service.payload.CodeOperation;
import com.nexis.websocket_service.service.lock.RedissonLockService;
import com.nexis.websocket_service.util.OTEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationalTransformService {

    private final RedissonLockService lockService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public CodeOperation processOperation(UUID workspaceId, CodeOperation incomingOp) {
        String historyKey = "nexis:workspace:" + workspaceId + ":history";
        String versionKey = "nexis:workspace:" + workspaceId + ":version";

        if (!lockService.acquireLock(workspaceId)) {
            log.warn("Lock collision: Could not acquire lock for workspace [{}] on operation [{}]", workspaceId, incomingOp.getOperationType());
            throw new WorkspaceBusyException("Server is processing too many concurrent edits. Please retry.");
        }

        try {

            String versionStr = (String) redisTemplate.opsForValue().get(versionKey);
            int serverVersion = (versionStr != null) ? Integer.parseInt(versionStr) : 0;

            List<Object> rawHistory = redisTemplate.opsForList().range(historyKey, 0, -1);
            if (rawHistory != null && !rawHistory.isEmpty()) {

                try {
                    List<CodeOperation> history = rawHistory.stream()
                            .map(obj -> objectMapper.convertValue(obj, CodeOperation.class))
                            .collect(Collectors.toList());

                    for (CodeOperation historicalOp : history) {
                        if (historicalOp.getVersion() > incomingOp.getVersion()) {
                            incomingOp = OTEngine.transform(incomingOp, historicalOp);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    log.error("CORRUPTION DETECTED: Failed to parse operation history for workspace [{}]. History might be corrupted.", workspaceId, e);
                    throw new RuntimeException("Document history corruption detected.");
                }
            }

            int newVersion = serverVersion + 1;
            incomingOp.setVersion(newVersion);


            redisTemplate.opsForValue().set(versionKey, String.valueOf(newVersion));
            redisTemplate.opsForList().rightPush(historyKey, incomingOp);
            redisTemplate.opsForList().trim(historyKey, -100, -1);

            log.info("[OT-ENGINE] Workspace: {} | Ver: {}->{} | Op: {} | Pos: {} | User: {}",
                    workspaceId, serverVersion, newVersion,
                    incomingOp.getOperationType(), incomingOp.getPosition(), incomingOp.getUserId());


            return incomingOp;

        } finally {
            lockService.releaseLock(workspaceId);
        }
    }
}
