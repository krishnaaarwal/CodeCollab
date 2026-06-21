package com.nexis.execution_service.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.nexis.execution_service.config.type.CodeLanguage;
import com.nexis.execution_service.config.type.StatusType;
import com.nexis.execution_service.entity.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class DockerExecutor {

    private final DockerClient dockerClient;

    public ExecutionResult execute(UUID jobId, UUID userId, UUID workspaceId, CodeLanguage codeLanguage, String code) {

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        ContainerSetup setup = getContainerSetup(codeLanguage, code);

        try {
            dockerClient.inspectImageCmd(setup.image()).exec();
        } catch (NotFoundException e) {
            log.warn("Image {} not found locally. Pulling from Docker Hub... This may take a minute.", setup.image());
            try {
                dockerClient.pullImageCmd(setup.image()).start().awaitCompletion();
                log.info("Successfully pulled image: {}", setup.image());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                stderr.append("Failed to pull required Docker image from Docker Hub.");
                return new ExecutionResult(jobId, userId, workspaceId, StatusType.FAILED, "", stderr.toString());
            }
        }
        // -----------------------------------------------------

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(134217728L)
                .withNetworkMode("none")
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)
                .withReadonlyRootfs(true)
                .withBinds(Bind.parse("/tmp:/tmp"));

        CreateContainerResponse container = dockerClient.createContainerCmd(setup.image())
                .withName("nexis-job-" + jobId.toString())
                .withCmd(setup.command())
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        log.info("Created Docker container: {} for Job: {}", containerId, jobId);

        dockerClient.startContainerCmd(containerId).exec();

        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (frame.getStreamType() == StreamType.STDOUT) {
                                stdout.append(new String(frame.getPayload()));
                            } else if (frame.getStreamType() == StreamType.STDERR) {
                                stderr.append(new String(frame.getPayload()));
                            }
                        }
                    })
                    .awaitCompletion(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            stderr.append("\nExecution timed out after 30 seconds.");
        }

        dockerClient.removeContainerCmd(containerId).withForce(true).exec();

        StatusType finalStatus = StatusType.COMPLETED;
        if (stderr.length() > 0) {
            finalStatus = StatusType.FAILED;
        }
        return new ExecutionResult(jobId, userId, workspaceId, finalStatus, stdout.toString(), stderr.toString());
    }

    private record ContainerSetup(String image, String[] command) {}

    private ContainerSetup getContainerSetup(CodeLanguage language, String code) {
        return switch (language) {
            case PYTHON -> new ContainerSetup(
                    "python:3.11-slim",
                    new String[]{"python", "-c", code}
            );
            case JAVASCRIPT -> new ContainerSetup(
                    "node:18-alpine",
                    new String[]{"node", "-e", code}
            );
            case JAVA -> new ContainerSetup(
                    "eclipse-temurin:17-alpine",
                    // Move to /tmp before writing the file
                    new String[]{"sh", "-c", "cd /tmp && echo '" + code.replace("'", "'\\''") + "' > Main.java && java Main.java"}
            );
            case CPP -> new ContainerSetup(
                    "gcc:13",
                    // Move to /tmp before writing and compiling
                    new String[]{"sh", "-c", "cd /tmp && echo '" + code.replace("'", "'\\''") + "' > main.cpp && g++ main.cpp -o main && ./main"}
            );
            case DART -> new ContainerSetup(
                    "dart:stable",
                    // Move to /tmp before writing
                    new String[]{"sh", "-c", "cd /tmp && echo '" + code.replace("'", "'\\''") + "' > main.dart && dart run main.dart"}
            );
        };
    }
}