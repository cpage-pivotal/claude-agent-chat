package org.tanzu.materialstarter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tanzu.claudecode.cf.ClaudeCodeExecutor;

import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatHealthController {

    private final Optional<ClaudeCodeExecutor> executor;

    public ChatHealthController(@Autowired(required = false) ClaudeCodeExecutor executor) {
        this.executor = Optional.ofNullable(executor);
    }

    @GetMapping("/health")
    public HealthResponse health() {
        if (executor.isEmpty()) {
            return new HealthResponse(false, "not configured", "Claude Code CLI is not configured. Please ensure ANTHROPIC_API_KEY environment variable is set.");
        }
        
        ClaudeCodeExecutor exec = executor.get();
        boolean available = exec.isAvailable();
        String version = available ? exec.getVersion() : "unavailable";
        String message = available ? "Claude Code CLI is ready" : "Claude Code CLI binary not found";
        
        return new HealthResponse(available, version, message);
    }

    public record HealthResponse(boolean available, String version, String message) {}
}

