# AI Assistant RAG Application

A Spring Boot application that implements a RAG (Retrieval-Augmented Generation) system with local vector database and Confluence integration.

## Features

- **Multi-LLM Support**: OpenAI, Moonshot (月之暗面), 阿里云百炼 (通义千问), DeepSeek, and any OpenAI-compatible API
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
LLM (OpenAI/Moonshot/DeepSeek/etc.)
    ↓
Answer + Sources
```

## Supported LLM Providers

| 提供商 | 模型示例 | 配置方式 |
|--------|----------|----------|
| **OpenAI** | gpt-4o-mini, gpt-4o, gpt-3.5-turbo | `OPENAI_API_KEY` |
| **Moonshot** (月之暗面) | moonshot-v1-8k, moonshot-v1-32k | `MOONSHOT_API_KEY` |
| **阿里云百炼** | qwen-turbo, qwen-plus, qwen-max | `DASHSCOPE_API_KEY` |
| **DeepSeek** | deepseek-chat, deepseek-coder | `DEEPSEEK_API_KEY` |
| **其他** | 任意兼容 OpenAI API 的模型 | 自定义配置 |

## Prerequisites

- Java 17+
- Maven 3.8+
- LLM API Key (OpenAI/Moonshot/DeepSeek/阿里云等)
- (Optional) Confluence instance with API access

## Configuration

### 1. OpenAI (默认)

```bash
export OPENAI_API_KEY=your-openai-api-key
export OPENAI_MODEL=gpt-4o-mini  # 可选: gpt-4o, gpt-3.5-turbo
```

### 2. Moonshot (月之暗面 - 推荐国内使用)

```bash
export OPENAI_API_KEY=your-moonshot-api-key
export OPENAI_MODEL=moonshot-v1-8k
export OPENAI_BASE_URL=https://api.moonshot.cn/v1
```

### 3. 阿里云百炼 (通义千问)

```bash
export OPENAI_API_KEY=your-dashscope-api-key
export OPENAI_MODEL=qwen-turbo
export OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

### 4. DeepSeek

```bash
export OPENAI_API_KEY=your-deepseek-api-key
export OPENAI_MODEL=deepseek-chat
export OPENAI_BASE_URL=https://api.deepseek.com/v1
```

### Confluence 配置 (可选)

```bash
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

## Testing

### 运行单元测试

```bash
mvn test
```

### 测试大模型调用

```bash
# 设置 API Key
export OPENAI_API_KEY=your-api-key

# 运行 LLM 调用测试
mvn test -Dtest=LLMProviderTest
```

### Confluence API 测试

项目包含使用 WireMock 模拟 Confluence REST API 的测试：

```bash
# 运行 Confluence 服务测试
mvn test -Dtest=ConfluenceServiceTest

# 运行 RAG Fallback 流程测试
mvn test -Dtest=RagFallbackFlowTest
```

**ConfluenceServiceTest** 包含以下测试场景：
- Confluence 启用/禁用状态检查
- 成功搜索 Confluence 页面
- 空结果处理
- API 错误处理（401, 500, 超时）
- HTML 内容清理
- 特殊字符查询处理
- 分页限制参数验证

**RagFallbackFlowTest** 包含以下测试场景：
- 本地有结果时不查询 Confluence
- 本地无结果时自动 fallback 到 Confluence
- 禁用 fallback 时的行为
- Confluence 未启用时的处理
- Confluence 查询失败时的优雅降级
- 多结果合并
- CQL 查询参数验证

### 手动测试真实 Confluence 连接

如果你需要测试与真实 Confluence 实例的连接：

```bash
# 1. 设置环境变量
export CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki
export CONFLUENCE_USERNAME=your-email@example.com
export CONFLUENCE_API_TOKEN=your-api-token
export CONFLUENCE_SPACE_KEY=YOURSPACE  # 可选

# 2. 运行测试脚本
./test-confluence.sh

# 或者带搜索关键词
./test-confluence.sh "Spring Boot"
```

**获取 API Token:**
1. 访问 https://id.atlassian.com/manage-profile/security/api-tokens
2. 点击 "Create API token"
3. 复制生成的 token 作为 `CONFLUENCE_API_TOKEN`

### 使用测试脚本

```bash
# 启动应用后运行
./test.sh
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

## Docker Support

```bash
# Build Docker image
docker build -t ai-assistant-rag .

# Run with Docker
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=your-api-key \
  -e OPENAI_MODEL=gpt-4o-mini \
  ai-assistant-rag
```

## License

MIT
