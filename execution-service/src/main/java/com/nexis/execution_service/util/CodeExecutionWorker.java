package com.nexis.execution_service.util;

import com.nexis.execution_service.config.RabbitMqConfig;
import com.nexis.execution_service.config.type.StatusType;
import com.nexis.execution_service.dto.JobMessageDto;
import com.nexis.execution_service.entity.ExecutionJob;
import com.nexis.execution_service.entity.ExecutionResult;
import com.nexis.execution_service.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodeExecutionWorker {
    private final ExecutionRepository executionRepository;
    private final DockerExecutor dockerExecutor;
    private final AmqpTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    public void consumeExecutionJob(JobMessageDto message) {
        log.info("Job picked up: " + message.getJobId());

        // Step 1: Fetch the ExecutionJob from the database using message.getJobId()
        ExecutionJob job = executionRepository.findById(message.getJobId())
                .orElseThrow(()->new IllegalArgumentException("Job not found"));

        // Step 2: Check if the status is FAILED (meaning the user killed it while it was in the queue). If it is, return early!
        if(job.getStatus()== StatusType.FAILED)
            return;

        // Step 3: Update the database status to PROCESSING and save it.
        job.setStatus(StatusType.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        
        executionRepository.save(job);

        // Step 4: Call dockerExecutor.execute(...) and get the ExecutionResult.
        ExecutionResult result = dockerExecutor.execute(job.getId(),job.getUserId(),job.getWorkspaceId(),job.getCodeLanguage(), job.getCode());
        result.setUserId(job.getUserId());
        result.setWorkspaceId(job.getWorkspaceId());

        // Step 5: Update the database with the result's stdout, stderr, and final status. Update completedAt. Save it.
        job.setStatus(result.getStatus());
        job.setOutput(result.getOutput());
        job.setError(result.getError());
        job.setCompletedAt(LocalDateTime.now());

        executionRepository.save(job);

        // Step 6: Send the final output to the user via WebSocket so they don't have to keep calling getJobStatus().
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RESULT_EXCHANGE,
                RabbitMqConfig.RESULT_ROUTING_KEY,
                result
        );
        log.info("Job {} completed and sent to WebSocket router", job.getId());
    }
}
