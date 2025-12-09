package org.tanzu.materialstarter;

import org.tanzu.claudecode.cf.ClaudeCodeExecutor;
import org.tanzu.claudecode.cf.ClaudeCodeExecutionException;
import org.tanzu.claudecode.cf.ClaudeCodeOptions;
import org.tanzu.claudecode.cf.ConversationSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller for managing conversational chat sessions with Claude Code CLI.
 * <p>
 * This controller provides endpoints for:
 * <ul>
 *   <li>Creating new conversation sessions</li>
 *   <li>Sending messages to existing sessions (with streaming responses)</li>
 *   <li>Closing conversation sessions</li>
 *   <li>Checking session status</li>
 * </ul>
 * </p>
 * 
 * <h3>Conversation Sessions</h3>
 * <p>
 * Each chat session maintains conversation context across multiple messages.
 * Sessions use the claude-code-cf-wrapper's conversation session API to enable
 * multi-turn conversations where Claude remembers previous exchanges.
 * </p>
 */
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
        logger.info("ClaudeChatController initialized with conversational session support");
    }

    /**
     * Create a new conversation session.
     * 
     * @param request optional session configuration
     * @return session ID and success status
     */
    @PostMapping("/sessions")
    public ResponseEntity<CreateSessionResponse> createSession(
            @RequestBody(required = false) CreateSessionRequest request) {
        logger.info("Creating new conversation session");
        
        try {
            if (!executor.isAvailable()) {
                logger.error("Claude Code CLI is not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new CreateSessionResponse(null, false, "Claude Code CLI is not available"));
            }

            ClaudeCodeOptions.Builder optionsBuilder = ClaudeCodeOptions.builder()
                .dangerouslySkipPermissions(true);

            // Apply custom configuration if provided
            if (request != null) {
                if (request.sessionInactivityTimeoutMinutes() != null) {
                    optionsBuilder.sessionInactivityTimeout(
                        Duration.ofMinutes(request.sessionInactivityTimeoutMinutes())
                    );
                }
                if (request.model() != null) {
                    optionsBuilder.model(request.model());
                }
            }

            ClaudeCodeOptions options = optionsBuilder.build();
            String sessionId = executor.createConversationSession(options);
            
            logger.info("Created conversation session: {}", sessionId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateSessionResponse(sessionId, true, null));
                
        } catch (Exception e) {
            logger.error("Failed to create conversation session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CreateSessionResponse(null, false, 
                    "Failed to create session: " + e.getMessage()));
        }
    }

    /**
     * Send a message to an existing session and stream the response via SSE.
     * 
     * @param sessionId the conversation session ID
     * @param request the message to send
     * @return SSE stream of response chunks
     */
    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @PathVariable String sessionId,
            @RequestBody SendMessageRequest request) {
        logger.info("Sending message to session {}: {} chars", sessionId, request.message().length());
        
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

                if (!executor.isSessionActive(sessionId)) {
                    logger.error("Session {} is not active or does not exist", sessionId);
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Session not found or has expired"));
                    emitter.complete();
                    return;
                }

                // Send message using conversation session (blocking mode for now)
                // Note: Streaming conversation sessions are a future enhancement in claude-code-cf-wrapper
                try {
                    String response = executor.sendMessage(sessionId, request.message());
                    
                    // Split response into lines for streaming effect
                    String[] lines = response.split("\n");
                    for (String line : lines) {
                        emitter.send(SseEmitter.event()
                            .name("message")
                            .data(line));
                    }
                    
                    emitter.complete();
                    logger.info("Message sent successfully to session {}", sessionId);
                    
                } catch (ConversationSessionManager.SessionNotFoundException e) {
                    logger.error("Session {} not found", sessionId, e);
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Session not found or has expired"));
                    emitter.completeWithError(e);
                } catch (ClaudeCodeExecutionException e) {
                    logger.error("Claude Code execution failed for session {}", sessionId, e);
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Execution failed: " + e.getMessage()));
                    emitter.completeWithError(e);
                }
            } catch (Exception e) {
                logger.error("Unexpected error during message send to session {}", sessionId, e);
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
            logger.warn("SSE connection timed out for session {}", sessionId);
            emitter.complete();
        });

        emitter.onError((e) -> {
            logger.error("SSE error for session {}", sessionId, e);
        });

        return emitter;
    }

    /**
     * Close a conversation session.
     * 
     * @param sessionId the session to close
     * @return success status
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<CloseSessionResponse> closeSession(@PathVariable String sessionId) {
        logger.info("Closing conversation session: {}", sessionId);
        
        try {
            executor.closeConversationSession(sessionId);
            logger.info("Session {} closed successfully", sessionId);
            return ResponseEntity.ok(
                new CloseSessionResponse(true, "Session closed successfully")
            );
        } catch (Exception e) {
            logger.error("Failed to close session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CloseSessionResponse(false, 
                    "Failed to close session: " + e.getMessage()));
        }
    }

    /**
     * Check if a session is active.
     * 
     * @param sessionId the session to check
     * @return session status
     */
    @GetMapping("/sessions/{sessionId}/status")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId) {
        logger.debug("Checking status for session: {}", sessionId);
        
        try {
            boolean active = executor.isSessionActive(sessionId);
            return ResponseEntity.ok(new SessionStatusResponse(sessionId, active));
        } catch (Exception e) {
            logger.error("Failed to check session status for {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SessionStatusResponse(sessionId, false));
        }
    }

    // Request/Response records
    
    public record CreateSessionRequest(
        String model,
        Integer sessionInactivityTimeoutMinutes
    ) {}

    public record CreateSessionResponse(
        String sessionId,
        boolean success,
        String message
    ) {}

    public record SendMessageRequest(String message) {}

    public record CloseSessionResponse(
        boolean success,
        String message
    ) {}

    public record SessionStatusResponse(
        String sessionId,
        boolean active
    ) {}
}

