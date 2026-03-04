# AI Assistant RAG Application

A Spring Boot application that implements a RAG (Retrieval-Augmented Generation) system with local vector database and Confluence integration.

## Features

- **Local Vector Database**: Uses SQLite with embeddings for local knowledge storage
- **Confluence Integration**: Falls back to Confluence search when local knowledge is insufficient
- **Multi-source RAG**: Combines local and Confluence knowledge for comprehensive answers
- **RESTful API**: Simple HTTP endpoints for chat and knowledge management

## Architecture

```
User Question
    ↓
Local Vector DB Search
    ↓
[If no results] → Confluence Search
    ↓
Combine Context
    ↓
LLM (OpenAI)
    ↓
Answer + Sources
```

## Prerequisites

- Java 17+
- Maven 3.8+
- OpenAI API Key
- (Optional) Confluence instance with API access

## Configuration

Set environment variables or edit `application.properties`:

```bash
# Required
export OPENAI_API_KEY=your-openai-api-key

# Optional - for Confluence integration
export CONFLUENCE_ENABLED=true
export CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki
export CONFLUENCE_USERNAME=your-email@example.com
export CONFLUENCE_API_TOKEN=your-api-token
export CONFLUENCE_SPACE_KEY=YOURSPACE
```

## Running the Application

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/ai-assistant-rag-1.0.0.jar
```

## API Endpoints

### Chat

```bash
# POST request
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the deployment process?",
    "sessionId": "optional-session-id",
    "useConfluenceFallback": true
  }'

# GET request
curl "http://localhost:8080/api/v1/chat?question=What is the deployment process?"
```

### Add Knowledge

```bash
curl -X POST http://localhost:8080/api/v1/knowledge \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Deployment Process",
    "content": "The deployment process involves...",
    "sourceType": "LOCAL"
  }'
```

### Search Knowledge

```bash
curl "http://localhost:8080/api/v1/knowledge/search?query=deployment&maxResults=5"
```

## Response Format

```json
{
  "answer": "Based on the documentation...",
  "sources": [
    {
      "sourceType": "LOCAL",
      "title": "Deployment Guide",
      "url": "",
      "relevanceScore": 0.95,
      "content": "Deployment process..."
    },
    {
      "sourceType": "CONFLUENCE",
      "title": "Release Process",
      "url": "https://...",
      "relevanceScore": 0.88,
      "content": "Release checklist..."
    }
  ],
  "sessionId": "uuid-here",
  "processingTimeMs": 2345
}
```

## Development

### Project Structure

```
src/main/java/com/example/aiassistant/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data transfer objects
├── model/          # Domain models
├── service/        # Business logic
└── AiAssistantApplication.java
```

### Running Tests

```bash
mvn test
```

## License

MIT
