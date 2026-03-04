# Docker Support

## Build Docker Image

```bash
docker build -t ai-assistant-rag .
```

## Run with Docker

```bash
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=your-key-here \
  -e CONFLUENCE_ENABLED=true \
  -e CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki \
  -e CONFLUENCE_USERNAME=your-email \
  -e CONFLUENCE_API_TOKEN=your-token \
  ai-assistant-rag
```

## Docker Compose

```yaml
version: '3.8'
services:
  ai-assistant:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - CONFLUENCE_ENABLED=${CONFLUENCE_ENABLED:-false}
      - CONFLUENCE_BASE_URL=${CONFLUENCE_BASE_URL}
      - CONFLUENCE_USERNAME=${CONFLUENCE_USERNAME}
      - CONFLUENCE_API_TOKEN=${CONFLUENCE_API_TOKEN}
    volumes:
      - ./data:/app/data
```
