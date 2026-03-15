#!/bin/bash
set -euo pipefail

WEAVIATE_URL="${WEAVIATE_URL:-http://localhost:8080}"

echo "=== Weaviate Troubleshooting ==="

# Check if Weaviate is running
echo -n "Weaviate container status: "
if command -v podman-compose &> /dev/null; then
    CONTAINER_CMD="podman-compose"
elif command -v docker-compose &> /dev/null; then
    CONTAINER_CMD="docker-compose"
elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
    CONTAINER_CMD="docker compose"
else
    echo "No container command found"
    exit 1
fi

CONTAINER_STATUS=$($CONTAINER_CMD -f docker-compose.dev.yml ps weaviate-dev 2>/dev/null | grep weaviate-dev || echo "not found")
if echo "$CONTAINER_STATUS" | grep -q "Up"; then
    echo "✓ Running"
else
    echo "✗ Not running or not found"
    echo "Container status: $CONTAINER_STATUS"
fi

# Check Weaviate connectivity
echo -n "Weaviate connectivity: "
if curl -s --max-time 5 "$WEAVIATE_URL/v1/meta" > /dev/null 2>&1; then
    echo "✓ Accessible"
else
    echo "✗ Not accessible"
fi

# Get Weaviate meta information
echo ""
echo "Weaviate Meta Information:"
META_RESPONSE=$(curl -s --max-time 5 "$WEAVIATE_URL/v1/meta" 2>/dev/null || echo "")
if [ -n "$META_RESPONSE" ]; then
    echo "$META_RESPONSE" | jq '.' 2>/dev/null || echo "$META_RESPONSE"
else
    echo "No response from meta endpoint"
fi

# Check current schema
echo ""
echo "Current Schema:"
SCHEMA_RESPONSE=$(curl -s --max-time 5 "$WEAVIATE_URL/v1/schema" 2>/dev/null || echo "")
if [ -n "$SCHEMA_RESPONSE" ]; then
    echo "$SCHEMA_RESPONSE" | jq '.classes[].class' 2>/dev/null || echo "No classes found"
else
    echo "No response from schema endpoint"
fi

# Check container logs
echo ""
echo "Recent Weaviate Logs (last 20 lines):"
$CONTAINER_CMD -f docker-compose.dev.yml logs --tail 20 weaviate-dev 2>/dev/null || echo "No logs available"

# Test schema creation
echo ""
echo "Testing minimal schema creation:"
MINIMAL_SCHEMA='{
  "class": "TestClass",
  "description": "Test class for troubleshooting",
  "vectorizer": "none",
  "properties": [
    {
      "name": "testProperty",
      "dataType": ["text"]
    }
  ]
}'

TEST_RESPONSE=$(curl -s -w "%{http_code}" -X POST "$WEAVIATE_URL/v1/schema" \
    -H "Content-Type: application/json" \
    -d "$MINIMAL_SCHEMA" 2>/dev/null || echo "000")

HTTP_CODE="${TEST_RESPONSE: -3}"
RESPONSE_BODY="${TEST_RESPONSE%???}"

case "$HTTP_CODE" in
    "200"|"201")
        echo "✓ Schema creation works"
        # Clean up test class
        curl -s -X DELETE "$WEAVIATE_URL/v1/schema/TestClass" > /dev/null 2>&1
        ;;
    "409")
        echo "✓ Schema endpoint works (class already exists)"
        ;;
    "422")
        echo "✗ Schema validation error (422)"
        echo "Response: $RESPONSE_BODY"
        ;;
    *)
        echo "✗ Unexpected response: $HTTP_CODE"
        echo "Response: $RESPONSE_BODY"
        ;;
esac

echo ""
echo "=== Troubleshooting Recommendations ==="
echo "1. If container is not running:"
echo "   ./stop-dev-services.sh && ./start-dev-services.sh"
echo ""
echo "2. If connectivity fails:"
echo "   Check if port 8080 is available: netstat -tuln | grep 8080"
echo ""
echo "3. If schema creation fails:"
echo "   Try manual initialization: ./infrastructure/weaviate/init-weaviate-dev.sh"
echo ""
echo "4. If issues persist:"
echo "   Reset Weaviate data: $CONTAINER_CMD -f docker-compose.dev.yml down -v weaviate_dev_data"
