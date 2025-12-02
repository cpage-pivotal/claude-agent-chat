# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a full-stack Claude Code Chat application combining:
- **Backend**: Spring Boot 3.5.5 with Java 21
- **Frontend**: Angular 20 with Material Design
- **Build System**: Maven with Frontend Maven Plugin integration
- **Core Feature**: Real-time chat interface that executes Claude Code CLI commands via Server-Sent Events (SSE)

The project uses a monorepo structure where the Angular frontend is located in `src/main/frontend/` and is built as part of the Maven lifecycle.

## Architecture

### Backend (Spring Boot)

**Main Classes:**
- `MaterialStarterApplication.java` - Spring Boot application entry point
- `ClaudeChatController.java` - REST controller for chat streaming via SSE
- `ChatHealthController.java` - Health check endpoint for Claude Code CLI availability
- `DiagnosticsController.java` - Diagnostics endpoint

**Key Dependencies:**
- `claude-code-cf-wrapper` (1.0.0) - Custom library that wraps Claude Code CLI execution
- Fetched from GCP Artifact Registry: `https://us-central1-maven.pkg.dev/cf-mcp/maven-public`
- Uses virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) for concurrent chat handling

**API Endpoints:**
- `POST /api/chat/stream` - Streams Claude Code responses via SSE
- `GET /api/chat/health` - Returns Claude Code CLI availability and version

**Configuration:**
- `application.properties` - Enable/disable Claude Code features
- `.claude-code-config.yml` - Claude Code CLI settings, model selection, MCP server configuration

### Frontend (Angular)

**Structure:**
- `src/main/frontend/src/app/components/chat/` - Main chat component
- `src/main/frontend/src/app/services/chat.service.ts` - Service for SSE streaming and health checks
- `src/main/frontend/src/app/pipes/` - Custom pipes for data transformation
- `proxy.conf.mjs` - Proxies `/api` requests to `localhost:8080` during development

**Key Features:**
- Standalone components (no NgModules)
- Material Design 3 with Azure primary palette
- Real-time SSE streaming for chat messages
- Markdown rendering support (using `marked` library)

## Development Commands

### Full Stack Development

```bash
# Build entire project (backend + frontend)
./mvnw clean package

# Run Spring Boot application (includes built frontend)
./mvnw spring-boot:run

# Run tests
./mvnw test

# Clean build
./mvnw clean
```

### Frontend Only (in src/main/frontend/)

```bash
# Install dependencies
npm ci

# Development server (http://localhost:4200)
# Uses proxy to forward /api requests to localhost:8080
npm start
# or
ng serve

# Build frontend
npm run build

# Run tests
ng test

# Watch mode for development
npm run watch
```

### Running Frontend and Backend Together

For local development, run both:
1. Start backend: `./mvnw spring-boot:run` (runs on port 8080)
2. Start frontend: `cd src/main/frontend && npm start` (runs on port 4200, proxies API calls to 8080)

## Key Technologies & Patterns

### Backend Patterns
- **Java 21**: Uses modern features (records for DTOs, virtual threads)
- **SSE Streaming**: `SseEmitter` with 5-minute timeout for real-time chat
- **ClaudeCodeExecutor**: Executes CLI commands with streaming output
- **Conditional Bean Loading**: Controllers use `@ConditionalOnProperty` for feature toggling

### Frontend Patterns
- **Angular 20**: Standalone components, signals, new control flow syntax
- **Material Design**: `mat.theme()` with system variables for theming
- **SSE Client**: Custom Observable-based implementation for streaming
- **Fetch API**: Used for HTTP requests instead of HttpClient
- **SCSS**: Material 3 design tokens via system variables

## Build Integration

The Maven build automatically:
1. Installs Node.js v22.12.0 and npm in `target/` (via Frontend Maven Plugin)
2. Runs `npm ci` to install frontend dependencies
3. Executes `ng build` to create production frontend build
4. Copies Angular build output from `src/main/frontend/dist/frontend/browser` to `target/classes/static`
5. Packages everything into a single Spring Boot JAR

## Configuration

### Claude Code CLI Configuration (.claude-code-config.yml)

Located in `src/main/resources/.claude-code-config.yml`:
- `claudeCode.enabled` - Enable/disable Claude Code
- `claudeCode.model` - Model selection (sonnet, opus, haiku)
- `claudeCode.settings.alwaysThinkingEnabled` - Enable thinking mode
- `claudeCode.mcpServers` - Configure MCP server connections (SSE or stdio)

### Application Properties

- `claude-code.enabled` - Master switch for Claude Code features
- `claude-code.controller-enabled` - Enable/disable controller endpoints
- Requires `ANTHROPIC_API_KEY` environment variable

## Code Style Preferences

- **Java**: Use records and lambdas where appropriate
- **Angular**: Use signals and new template control flow syntax
- **Material UI**: Follow Material Design 3 standards and system variables
- **Formatting**: Prettier configured for consistent code style