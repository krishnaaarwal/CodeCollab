package com.nexis.execution_service.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.nexis.execution_service.config.RabbitMqConfig;
import com.nexis.execution_service.dto.JobMessageDto;
import com.nexis.execution_service.dto.JobRequestDto;
import com.nexis.execution_service.dto.JobResponseDto;
import com.nexis.execution_service.entity.ExecutionJob;
import com.nexis.execution_service.repository.ExecutionRepository;
import com.nexis.execution_service.service.ExecutionService;
import com.nexis.execution_service.config.type.StatusType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;



@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionServiceImplementation implements ExecutionService {

    private final ExecutionRepository executionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DockerClient dockerClient;

    @Override
    public JobResponseDto submitJob(JobRequestDto requestDto) {

        ExecutionJob job = ExecutionJob.builder()
                .userId(requestDto.getUserId())
                .workspaceId(requestDto.getWorkspaceId())
                .codeLanguage(requestDto.getCodeLanguage())
                .code(requestDto.getCode())
                .status(StatusType.QUEUED)
                .createdAt(LocalDateTime.now())
                .build();

        ExecutionJob savedJob = executionRepository.save(job);

        JobMessageDto messageDto = new JobMessageDto(savedJob.getId(), savedJob.getCodeLanguage());
        
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, RabbitMqConfig.ROUTING_KEY, messageDto);

        return new JobResponseDto(savedJob.getId(), savedJob.getStatus(), savedJob.getOutput(), savedJob.getError(), null);
    }

    @Override
    @Transactional
    public Void killJob(UUID jobId) {
        ExecutionJob job = executionRepository.findById(jobId)
                .orElseThrow(()->new IllegalArgumentException("Job not found!"+jobId));

        job.setStatus(StatusType.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        executionRepository.save(job);


        String containerName = "nexis-job-" + jobId;
        try {
            dockerClient.removeContainerCmd(containerName).withForce(true).exec();
            log.info("Forcefully destroyed Docker container: {}", containerName);

        } catch (com.github.dockerjava.api.exception.NotFoundException e) {
            // Why do we catch this?
            // If the user clicks "Kill" while the job is still QUEUED (Worker hasn't picked it up yet),
            // the container doesn't exist. Docker will throw a NotFoundException. We catch it and safely ignore it.
            log.info("Container {} not found. It was either already finished or never started.", containerName);

        } catch (Exception e) {
            log.error("Failed to kill container {}: {}", containerName, e.getMessage());
        }

        return null;
    }

    @Override
    @Transactional
    public JobResponseDto getJobStatus(UUID jobId) {
        ExecutionJob job = executionRepository.findById(jobId).orElseThrow(()->new IllegalArgumentException("Job not found!"+jobId));

        long durationMs = 0;
        if(job.getStartedAt() != null && job.getCompletedAt() != null){
            durationMs = Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis();
        }
        return new JobResponseDto(job.getId(),job.getStatus(),job.getOutput(), job.getError(),durationMs);
    }

}
