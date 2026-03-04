#!/bin/bash

# AI Assistant RAG - Functional Test Script
# This script tests the basic functionality of the application

set -e

echo "=========================================="
echo "AI Assistant RAG - Functional Test Script"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080/api/v1"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Test 1: Health Check
echo "Test 1: Health Check"
echo "--------------------"
if curl -s -f "${BASE_URL}/health" > /dev/null 2>&1; then
    print_success "Health check passed"
else
    print_error "Health check failed - Is the application running?"
    echo "Start the app with: mvn spring-boot:run"
    exit 1
fi
echo ""

# Test 2: Add Knowledge Document
echo "Test 2: Add Knowledge Document"
echo "------------------------------"
RESPONSE=$(curl -s -X POST "${BASE_URL}/knowledge" \
    -H "Content-Type: application/json" \
    -d '{
        "title": "Spring Boot Introduction",
        "content": "Spring Boot is an open source Java-based framework used to create a micro Service. It is developed by Pivotal Team and is used to build stand-alone and production ready spring applications.",
        "sourceType": "LOCAL"
    }' 2>/dev/null)

if echo "$RESPONSE" | grep -q "successfully"; then
    print_success "Knowledge document added"
else
    print_error "Failed to add knowledge document"
    echo "Response: $RESPONSE"
fi
echo ""

# Test 3: Search Knowledge
echo "Test 3: Search Knowledge"
echo "------------------------"
RESPONSE=$(curl -s "${BASE_URL}/knowledge/search?query=Spring%20Boot&maxResults=5" 2>/dev/null)

if echo "$RESPONSE" | grep -q "Spring Boot"; then
    print_success "Knowledge search working"
    echo "Results: $RESPONSE" | head -c 200
    echo "..."
else
    print_error "Knowledge search failed"
    echo "Response: $RESPONSE"
fi
echo ""
echo ""

# Test 4: Chat (requires OpenAI API key)
echo "Test 4: Chat API"
echo "----------------"
echo "Note: This test requires a valid OpenAI API key"

RESPONSE=$(curl -s -X POST "${BASE_URL}/chat" \
    -H "Content-Type: application/json" \
    -d '{
        "question": "What is Spring Boot?",
        "useConfluenceFallback": false
    }' 2>/dev/null)

if echo "$RESPONSE" | grep -q "answer"; then
    print_success "Chat API working"
    echo "Response preview:"
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
    print_error "Chat API failed (might need OpenAI API key)"
    echo "Response: $RESPONSE"
fi
echo ""

# Test 5: Chat with GET method
echo "Test 5: Chat API (GET method)"
echo "-----------------------------"
RESPONSE=$(curl -s "${BASE_URL}/chat?question=What%20is%20micro%20service?&useConfluenceFallback=false" 2>/dev/null)

if echo "$RESPONSE" | grep -q "answer\|error"; then
    print_success "Chat GET API responding"
else
    print_error "Chat GET API not responding correctly"
fi
echo ""

echo "=========================================="
echo "Functional tests completed!"
echo "=========================================="
echo ""
echo "To run the application:"
echo "  export OPENAI_API_KEY=your-key-here"
echo "  mvn spring-boot:run"
echo ""
echo "API Documentation:"
echo "  POST ${BASE_URL}/chat           - Ask a question"
echo "  GET  ${BASE_URL}/chat           - Ask a question (query params)"
echo "  POST ${BASE_URL}/knowledge      - Add knowledge document"
echo "  GET  ${BASE_URL}/knowledge/search - Search knowledge base"
echo "  GET  ${BASE_URL}/health         - Health check"
