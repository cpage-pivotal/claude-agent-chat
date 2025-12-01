package org.tanzu.materialstarter;

import org.tanzu.claudecode.cf.ClaudeCodeExecutor;
import org.tanzu.claudecode.cf.ClaudeCodeExecutionException;
import org.tanzu.claudecode.cf.ClaudeCodeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "claude-code.enabled", havingValue = "true", matchIfMissing = true)
public class ClaudeChatController {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeChatController.class);
    private final ClaudeCodeExecutor executor;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public ClaudeChatController(ClaudeCodeExecutor executor) {
        this.executor = executor;
        logger.info("ClaudeChatController initialized with executor");
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        logger.info("Received chat request: {}", request.prompt());
        
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minutes timeout
        
        executorService.execute(() -> {
            try {
                if (!executor.isAvailable()) {
                    logger.error("Claude Code CLI is not available");
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Claude Code CLI is not available"));
                    emitter.complete();
                    return;
                }

                ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                    .timeout(Duration.ofMinutes(4))
                    .dangerouslySkipPermissions(true)
                    .build();

                try (Stream<String> lines = executor.executeStreaming(request.prompt(), options)) {
                    lines.forEach(line -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("message")
                                .data(line));
                        } catch (IOException e) {
                            logger.error("Error sending SSE message", e);
                            throw new RuntimeException(e);
                        }
                    });
                    emitter.complete();
                    logger.info("Chat request completed successfully");
                } catch (ClaudeCodeExecutionException e) {
                    logger.error("Claude Code execution failed", e);
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Execution failed: " + e.getMessage()));
                    emitter.completeWithError(e);
                }
            } catch (Exception e) {
                logger.error("Unexpected error during chat streaming", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("An unexpected error occurred"));
                } catch (IOException ex) {
                    logger.error("Failed to send error event", ex);
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out");
            emitter.complete();
        });

        emitter.onError((e) -> {
            logger.error("SSE error", e);
        });

        return emitter;
    }

    public record ChatRequest(String prompt) {}
}

