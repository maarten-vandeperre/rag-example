#!/bin/bash
set -euo pipefail

WEAVIATE_URL="${WEAVIATE_URL:-http://localhost:8080}"
SCHEMA_FILE="${SCHEMA_FILE:-infrastructure/weaviate/dev-schema.json}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
ATTEMPT=0

echo "=== Initializing Weaviate Development Setup ==="

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
    echo "ERROR: curl and jq are required"
    exit 1
fi

if [ ! -f "$SCHEMA_FILE" ]; then
    echo "ERROR: Schema file not found: $SCHEMA_FILE"
    echo "Creating minimal schema file..."
    mkdir -p "$(dirname "$SCHEMA_FILE")"
    cat > "$SCHEMA_FILE" <<'EOF'
{
  "classes": [
    {
      "class": "DocumentChunk",
      "description": "A chunk of text from an uploaded document",
      "vectorizer": "none",
      "properties": [
        {
          "name": "textContent",
          "dataType": ["text"],
          "description": "The text content"
        },
        {
          "name": "documentId",
          "dataType": ["text"],
          "description": "Document ID"
        }
      ]
    }
  ]
}
EOF
    echo "✓ Created minimal schema file"
fi

# Wait for Weaviate to be ready
echo "Waiting for Weaviate to be ready..."
while [ "$ATTEMPT" -lt "$MAX_ATTEMPTS" ]; do
    if curl -s --max-time 5 "${WEAVIATE_URL}/v1/meta" > /dev/null 2>&1; then
        echo " ✓ Weaviate is responding"
        break
    fi
    echo -n "."
    sleep 3
    ATTEMPT=$((ATTEMPT + 1))
done

if [ "$ATTEMPT" -eq "$MAX_ATTEMPTS" ]; then
    echo " ✗"
    echo "ERROR: Weaviate is not responding after waiting"
    echo "Checking Weaviate container status..."
    if command -v podman-compose &> /dev/null; then
        podman-compose -f docker-compose.dev.yml logs --tail 20 weaviate-dev
    else
        echo "podman-compose is required to inspect Weaviate logs"
    fi
    exit 1
fi

# Additional wait for Weaviate to be fully initialized
echo "Waiting for Weaviate to be fully initialized..."
sleep 10

# Check Weaviate readiness
echo "Checking Weaviate readiness..."
META_RESPONSE=$(curl -s "${WEAVIATE_URL}/v1/meta" 2>/dev/null || echo "")
if ! echo "$META_RESPONSE" | grep -q "hostname"; then
    echo "ERROR: Weaviate meta endpoint not returning expected data"
    echo "Response: $META_RESPONSE"
    exit 1
fi

echo "✓ Weaviate is ready"

echo "Checking existing schema..."
EXISTING_SCHEMA=$(curl -s "${WEAVIATE_URL}/v1/schema" 2>/dev/null || echo '{"classes":[]}')
EXISTING_CLASSES=$(echo "$EXISTING_SCHEMA" | jq -r '.classes[]?.class // empty' 2>/dev/null || echo "")

if echo "$EXISTING_CLASSES" | grep -q "DocumentChunk"; then
    echo "✓ DocumentChunk class already exists"
else
    echo "Creating Weaviate schema..."
    if ! jq empty "$SCHEMA_FILE" > /dev/null 2>&1; then
        echo "ERROR: Invalid JSON in schema file: $SCHEMA_FILE"
        exit 1
    fi

    SCHEMA_RESPONSE=$(curl -s -w "%{http_code}" -X POST "${WEAVIATE_URL}/v1/schema" \
        -H "Content-Type: application/json" \
        -d @"$SCHEMA_FILE" 2>/dev/null || echo "000")

    HTTP_CODE="${SCHEMA_RESPONSE: -3}"
    RESPONSE_BODY="${SCHEMA_RESPONSE%???}"

    case "$HTTP_CODE" in
        "200"|"201")
            echo "✓ Schema created successfully"
            ;;
        "409")
            echo "⚠ Schema already exists (409)"
            ;;
        "422")
            if echo "$RESPONSE_BODY" | grep -qi "already exist"; then
                echo "⚠ Schema already exists (422)"
            else
                echo "ERROR: Schema validation failed (422)"
                echo "Response: $RESPONSE_BODY"
                echo "Trying to create a minimal schema instead..."

                MINIMAL_SCHEMA='{"class":"DocumentChunk","description":"Simple document chunk","vectorizer":"none","properties":[{"name":"textContent","dataType":["text"]},{"name":"documentId","dataType":["text"]}]}'
                MINIMAL_RESPONSE=$(curl -s -w "%{http_code}" -X POST "${WEAVIATE_URL}/v1/schema" \
                    -H "Content-Type: application/json" \
                    -d "$MINIMAL_SCHEMA" 2>/dev/null || echo "000")
                MINIMAL_HTTP_CODE="${MINIMAL_RESPONSE: -3}"
                if [ "$MINIMAL_HTTP_CODE" = "200" ] || [ "$MINIMAL_HTTP_CODE" = "201" ] || [ "$MINIMAL_HTTP_CODE" = "409" ]; then
                    echo "✓ Minimal schema created successfully"
                else
                    echo "ERROR: Even minimal schema creation failed"
                    echo "Response: ${MINIMAL_RESPONSE%???}"
                    exit 1
                fi
            fi
            ;;
        *)
            echo "ERROR: Unexpected response code: $HTTP_CODE"
            echo "Response: $RESPONSE_BODY"
            exit 1
            ;;
    esac
fi

# Verify schema creation
echo "Verifying schema..."
FINAL_SCHEMA=$(curl -s "${WEAVIATE_URL}/v1/schema" 2>/dev/null || echo '{"classes":[]}')
DOCUMENT_CHUNK_EXISTS=$(echo "$FINAL_SCHEMA" | jq -r '.classes[] | select(.class == "DocumentChunk") | .class' 2>/dev/null || echo "")

if [ "$DOCUMENT_CHUNK_EXISTS" = "DocumentChunk" ]; then
    echo "✓ DocumentChunk class verified"

    ensure_property() {
        local property_name="$1"
        if echo "$FINAL_SCHEMA" | jq -e --arg property_name "$property_name" '.classes[] | select(.class == "DocumentChunk") | .properties[]? | select(.name == $property_name)' >/dev/null 2>&1; then
            return 0
        fi

        local property_definition
        property_definition=$(jq -c --arg property_name "$property_name" '.classes[] | select(.class == "DocumentChunk") | .properties[] | select(.name == $property_name)' "$SCHEMA_FILE")
        if [ -z "$property_definition" ]; then
            echo "ERROR: Missing property definition for $property_name in $SCHEMA_FILE"
            exit 1
        fi

        echo "Adding missing property: $property_name"
        PROPERTY_RESPONSE=$(curl -s -w "%{http_code}" -X POST "${WEAVIATE_URL}/v1/schema/DocumentChunk/properties" \
            -H "Content-Type: application/json" \
            -d "$property_definition" 2>/dev/null || echo "000")
        PROPERTY_HTTP_CODE="${PROPERTY_RESPONSE: -3}"
        PROPERTY_BODY="${PROPERTY_RESPONSE%???}"

        case "$PROPERTY_HTTP_CODE" in
            "200"|"201")
                echo "✓ Added property $property_name"
                ;;
            "422")
                if echo "$PROPERTY_BODY" | grep -qi "already exist"; then
                    echo "⚠ Property $property_name already exists"
                else
                    echo "ERROR: Failed to add property $property_name"
                    echo "Response: $PROPERTY_BODY"
                    exit 1
                fi
                ;;
            *)
                echo "ERROR: Unexpected response while adding property $property_name: $PROPERTY_HTTP_CODE"
                echo "Response: $PROPERTY_BODY"
                exit 1
                ;;
        esac
    }

    for required_property in chunkIndex uploadedBy fileName fileType createdAt chunkSize; do
        ensure_property "$required_property"
    done

    FINAL_SCHEMA=$(curl -s "${WEAVIATE_URL}/v1/schema" 2>/dev/null || echo '{"classes":[]}')
    
    # Show class properties
    PROPERTIES=$(echo "$FINAL_SCHEMA" | jq -r '.classes[] | select(.class == "DocumentChunk") | .properties[].name' 2>/dev/null || echo "")
    if [ -n "$PROPERTIES" ]; then
        echo "Properties:"
        echo "$PROPERTIES" | while read -r prop; do
            if [ -n "$prop" ]; then
                echo "  - $prop"
            fi
        done
    fi
else
    echo "ERROR: DocumentChunk class not found after creation"
    echo "Available classes:"
    echo "$FINAL_SCHEMA" | jq -r '.classes[].class' 2>/dev/null || echo "None"
    exit 1
fi

echo ""
echo "=== Weaviate Development Setup Complete ==="
echo "Weaviate URL: ${WEAVIATE_URL}"
echo "GraphQL Endpoint: ${WEAVIATE_URL}/v1/graphql"
echo "REST API: ${WEAVIATE_URL}/v1"
echo ""
echo "Available classes:"
echo "$FINAL_SCHEMA" | jq -r '.classes[].class' 2>/dev/null || echo "None"
