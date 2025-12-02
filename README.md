# Claude Code Chat

A full-stack web application that provides a chat interface for interacting with Claude Code CLI directly from your browser. Built with Spring Boot and Angular, featuring real-time streaming responses and a modern Material Design interface.

## Features

- **Real-time Chat Interface** - Interactive chat with Claude Code using Server-Sent Events (SSE) for streaming responses
- **Modern UI** - Material Design 3 interface with Angular 20
- **Markdown Support** - Renders Claude's responses with proper markdown formatting
- **Health Monitoring** - Built-in health checks to verify Claude Code CLI availability
- **Monorepo Architecture** - Single Maven build produces a deployable JAR containing both frontend and backend
- **MCP Server Support** - Configurable Model Context Protocol server integration

## Prerequisites

- **Java 21** or higher
- **Maven 3.8+** (or use the included Maven wrapper `./mvnw`)
- **ANTHROPIC_API_KEY** environment variable set with your Anthropic API key
- **Claude Code CLI** installed and available in your PATH

## Quick Start

### 1. Set Your API Key

```bash
export ANTHROPIC_API_KEY=your_api_key_here
```

### 2. Build and Run

```bash
# Build the entire application (backend + frontend)
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

### 3. Access the Application

Open your browser to [http://localhost:8080](http://localhost:8080)

## Development Setup

For active development with hot-reload capabilities:

### Backend Development

```bash
# Terminal 1: Run Spring Boot backend on port 8080
./mvnw spring-boot:run
```

### Frontend Development

```bash
# Terminal 2: Run Angular dev server on port 4200
cd src/main/frontend
npm ci
npm start
```

The Angular dev server will proxy API requests to the Spring Boot backend running on port 8080.

### Running Tests

```bash
# Run all Spring Boot tests
./mvnw test

# Run Angular tests
cd src/main/frontend
ng test
```

## Project Structure

```
material-starter/
├── src/main/
│   ├── java/org/tanzu/materialstarter/
│   │   ├── MaterialStarterApplication.java    # Main application
│   │   ├── ClaudeChatController.java          # Chat streaming endpoint
│   │   ├── ChatHealthController.java          # Health check endpoint
│   │   └── DiagnosticsController.java         # Diagnostics
│   ├── frontend/                               # Angular application
│   │   ├── src/app/
│   │   │   ├── components/chat/               # Chat component
│   │   │   ├── services/                      # Services (SSE streaming)
│   │   │   └── pipes/                         # Custom pipes
│   │   └── proxy.conf.mjs                     # Dev proxy configuration
│   └── resources/
│       ├── application.properties             # Spring Boot config
│       └── .claude-code-config.yml            # Claude Code CLI config
└── pom.xml                                     # Maven build configuration
```

## Configuration

### Claude Code Settings

Edit `src/main/resources/.claude-code-config.yml`:

```yaml
claudeCode:
  enabled: true
  version: "latest"
  logLevel: info
  model: sonnet  # Options: sonnet, opus, haiku

  settings:
    alwaysThinkingEnabled: true

  mcpServers:
    - name: github
      type: sse
      url: "https://your-mcp-server-url/sse"
```

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
spring.application.name=material-starter
claude-code.enabled=true
claude-code.controller-enabled=false
```

## API Endpoints

- **POST /api/chat/stream** - Send a message and receive streaming SSE responses
- **GET /api/chat/health** - Check Claude Code CLI availability and version

## Architecture

### Backend (Spring Boot)

- **Java 21** with modern features (records, virtual threads)
- **SSE Streaming** via `SseEmitter` for real-time responses
- **claude-code-cf-wrapper** library for Claude Code CLI integration
- **Virtual Threads** for efficient concurrent request handling

### Frontend (Angular)

- **Angular 20** with standalone components
- **Material Design 3** theming with Azure palette
- **Custom SSE Client** using Fetch API and Observables
- **Marked.js** for markdown rendering

## Build Process

The Maven build automatically:

1. Installs Node.js v22.12.0 locally in `target/`
2. Runs `npm ci` to install frontend dependencies
3. Builds the Angular application with `ng build`
4. Copies the frontend build to Spring Boot's static resources
5. Packages everything into a single executable JAR

## Deployment

### Building for Production

```bash
./mvnw clean package
```

This creates an executable JAR at `target/claude-agent-chat-1.0.0.jar`

### Running in Production

```bash
export ANTHROPIC_API_KEY=your_api_key_here
java -jar target/claude-agent-chat-1.0.0.jar
```

The application will be available at `http://localhost:8080`

## Technologies Used

### Backend
- Spring Boot 3.5.5
- Java 21
- Spring Web (REST endpoints)
- Spring Actuator (health checks)
- Claude Code CF Wrapper

### Frontend
- Angular 20
- Angular Material
- TypeScript 5.9
- RxJS 7.8
- Marked (markdown rendering)

### Build & Tooling
- Maven 3
- Frontend Maven Plugin
- Prettier (code formatting)
- Karma & Jasmine (testing)

## Troubleshooting

### Claude Code CLI Not Found

Ensure Claude Code CLI is installed and in your PATH:

```bash
claude-code --version
```

### API Key Issues

Verify your API key is set:

```bash
echo $ANTHROPIC_API_KEY
```

### Port Already in Use

If port 8080 or 4200 is in use, you can change them:

```bash
# Backend: Add to application.properties
server.port=9090

# Frontend: Use --port flag
ng serve --port=4300
```

## License

This project is provided as a starter template for building Claude Code chat applications.

## Support

For issues related to:
- **Claude Code CLI**: Visit [Claude Code documentation](https://claude.ai/code)
- **This project**: Open an issue in the repository
