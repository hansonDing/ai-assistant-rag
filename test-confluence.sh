#!/bin/bash

# Confluence 连接测试脚本
# 用于验证真实 Confluence 实例的连接和搜索功能

set -e

echo "=========================================="
echo "Confluence 连接测试脚本"
echo "=========================================="
echo ""

# 检查环境变量
if [ -z "$CONFLUENCE_BASE_URL" ]; then
    echo "❌ 错误: CONFLUENCE_BASE_URL 未设置"
    echo "   请设置环境变量，例如:"
    echo "   export CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki"
    exit 1
fi

if [ -z "$CONFLUENCE_USERNAME" ]; then
    echo "❌ 错误: CONFLUENCE_USERNAME 未设置"
    echo "   请设置环境变量，例如:"
    echo "   export CONFLUENCE_USERNAME=your-email@example.com"
    exit 1
fi

if [ -z "$CONFLUENCE_API_TOKEN" ]; then
    echo "❌ 错误: CONFLUENCE_API_TOKEN 未设置"
    echo "   请设置环境变量，例如:"
    echo "   export CONFLUENCE_API_TOKEN=your-api-token"
    echo ""
    echo "💡 提示: API Token 可以在以下地址生成:"
    echo "   https://id.atlassian.com/manage-profile/security/api-tokens"
    exit 1
fi

echo "✓ 环境变量检查通过"
echo ""
echo "配置信息:"
echo "  Base URL: $CONFLUENCE_BASE_URL"
echo "  Username: $CONFLUENCE_USERNAME"
echo "  Space Key: ${CONFLUENCE_SPACE_KEY:-'(未设置，将搜索所有空间)'}"
echo ""

# 构建认证头
AUTH=$(echo -n "$CONFLUENCE_USERNAME:$CONFLUENCE_API_TOKEN" | base64)

echo "=========================================="
echo "测试1: 验证 API 连接"
echo "=========================================="
echo ""

# 测试获取当前用户信息
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Basic $AUTH" \
    -H "Accept: application/json" \
    "$CONFLUENCE_BASE_URL/rest/api/user/current" 2>/dev/null || echo "000")

if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ API 连接成功 (HTTP 200)"
    
    # 获取用户信息
    USER_INFO=$(curl -s \
        -H "Authorization: Basic $AUTH" \
        -H "Accept: application/json" \
        "$CONFLUENCE_BASE_URL/rest/api/user/current" 2>/dev/null)
    
    USERNAME=$(echo "$USER_INFO" | grep -o '"username":"[^"]*"' | cut -d'"' -f4)
    echo "  当前用户: $USERNAME"
else
    echo "❌ API 连接失败 (HTTP $HTTP_CODE)"
    echo "   请检查:"
    echo "   1. Base URL 是否正确"
    echo "   2. Username 和 API Token 是否正确"
    echo "   3. 网络连接是否正常"
    exit 1
fi

echo ""
echo "=========================================="
echo "测试2: 搜索内容"
echo "=========================================="
echo ""

# 构建 CQL 查询
SEARCH_QUERY="${1:-test}"
if [ -n "$CONFLUENCE_SPACE_KEY" ]; then
    CQL="space = $CONFLUENCE_SPACE_KEY AND text ~ \"$SEARCH_QUERY\""
else
    CQL="text ~ \"$SEARCH_QUERY\""
fi

echo "搜索关键词: $SEARCH_QUERY"
echo "CQL 查询: $CQL"
echo ""

# URL 编码 CQL
ENCODED_CQL=$(echo "$CQL" | python3 -c "import sys,urllib.parse; print(urllib.parse.quote(sys.stdin.read().strip()))" 2>/dev/null || echo "$CQL")

# 执行搜索
SEARCH_RESULT=$(curl -s \
    -H "Authorization: Basic $AUTH" \
    -H "Accept: application/json" \
    "$CONFLUENCE_BASE_URL/rest/api/content/search?cql=$ENCODED_CQL&limit=5" 2>/dev/null)

# 检查结果
RESULT_COUNT=$(echo "$SEARCH_RESULT" | grep -o '"results":\[' | wc -l)

if [ -n "$SEARCH_RESULT" ] && echo "$SEARCH_RESULT" | grep -q '"results"'; then
    # 解析结果数量
    SIZE=$(echo "$SEARCH_RESULT" | python3 -c "import sys,json; data=json.load(sys.stdin); print(len(data.get('results', [])))" 2>/dev/null || echo "0")
    
    echo "✓ 搜索成功"
    echo "  找到 $SIZE 个结果"
    echo ""
    
    if [ "$SIZE" -gt 0 ]; then
        echo "搜索结果预览:"
        echo "$SEARCH_RESULT" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for i, result in enumerate(data.get('results', [])[:3], 1):
    title = result.get('title', 'N/A')
    page_id = result.get('id', 'N/A')
    type_ = result.get('type', 'N/A')
    print(f'  {i}. {title}')
    print(f'     ID: {page_id}, Type: {type_}')
" 2>/dev/null || echo "  (无法解析结果)"
    fi
else
    echo "⚠️  搜索返回空结果或发生错误"
    echo "   响应: $(echo "$SEARCH_RESULT" | head -c 200)"
fi

echo ""
echo "=========================================="
echo "测试3: 获取空间列表 (如果设置了 Space Key)"
echo "=========================================="
echo ""

if [ -n "$CONFLUENCE_SPACE_KEY" ]; then
    SPACE_RESULT=$(curl -s \
        -H "Authorization: Basic $AUTH" \
        -H "Accept: application/json" \
        "$CONFLUENCE_BASE_URL/rest/api/space/$CONFLUENCE_SPACE_KEY" 2>/dev/null)
    
    if echo "$SPACE_RESULT" | grep -q '"key"'; then
        SPACE_NAME=$(echo "$SPACE_RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('name','N/A'))" 2>/dev/null)
        echo "✓ 空间 '$CONFLUENCE_SPACE_KEY' 存在"
        echo "  空间名称: $SPACE_NAME"
    else
        echo "⚠️  空间 '$CONFLUENCE_SPACE_KEY' 不存在或无法访问"
    fi
else
    echo "ℹ️  未设置 CONFLUENCE_SPACE_KEY，跳过空间检查"
    echo "   可以设置特定空间进行搜索:"
    echo "   export CONFLUENCE_SPACE_KEY=YOURSPACE"
fi

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
