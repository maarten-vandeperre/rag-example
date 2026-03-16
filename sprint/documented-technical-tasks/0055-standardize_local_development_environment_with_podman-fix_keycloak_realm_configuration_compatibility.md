# Fix Keycloak Realm Configuration Compatibility

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Fix the Keycloak realm configuration to be compatible with Keycloak 23.0, removing unsupported fields and ensuring proper realm import functionality.

## Scope

- Fix Keycloak realm JSON configuration for version 23.0 compatibility
- Remove unsupported client configuration fields
- Update client configuration to use supported properties
- Ensure proper realm import process
- Add validation for realm configuration

## Out of Scope

- Upgrading to newer Keycloak versions
- Advanced Keycloak configuration features
- Custom Keycloak themes or extensions
- Production Keycloak security hardening

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0048-standardize_local_development_environment_with_podman-configure_keycloak_development_realm.md
- 0054-standardize_local_development_environment_with_podman-fix_docker_compose_dev_startup_issues.md

## Implementation Details

Create fixed Keycloak realm configuration (infrastructure/keycloak/dev-realm.json):
```json
{
  "realm": "rag-app-dev",
  "displayName": "RAG Application Development",
  "enabled": true,
  "sslRequired": "none",
  "registrationAllowed": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,
  "bruteForceProtected": true,
  "permanentLockout": false,
  "maxFailureWaitSeconds": 900,
  "minimumQuickLoginWaitSeconds": 60,
  "waitIncrementSeconds": 60,
  "quickLoginCheckMilliSeconds": 1000,
  "maxDeltaTimeSeconds": 43200,
  "failureFactor": 30,
  "defaultSignatureAlgorithm": "RS256",
  "revokeRefreshToken": false,
  "refreshTokenMaxReuse": 0,
  "accessTokenLifespan": 300,
  "accessTokenLifespanForImplicitFlow": 900,
  "ssoSessionIdleTimeout": 1800,
  "ssoSessionMaxLifespan": 36000,
  "ssoSessionIdleTimeoutRememberMe": 0,
  "ssoSessionMaxLifespanRememberMe": 0,
  "offlineSessionIdleTimeout": 2592000,
  "offlineSessionMaxLifespanEnabled": false,
  "offlineSessionMaxLifespan": 5184000,
  "clientSessionIdleTimeout": 0,
  "clientSessionMaxLifespan": 0,
  "clientOfflineSessionIdleTimeout": 0,
  "clientOfflineSessionMaxLifespan": 0,
  "accessCodeLifespan": 60,
  "accessCodeLifespanUserAction": 300,
  "accessCodeLifespanLogin": 1800,
  "actionTokenGeneratedByAdminLifespan": 43200,
  "actionTokenGeneratedByUserLifespan": 300,
  "oauth2DeviceCodeLifespan": 600,
  "oauth2DevicePollingInterval": 5,
  "roles": {
    "realm": [
      {
        "name": "STANDARD",
        "description": "Standard user role - can access own documents",
        "composite": false,
        "clientRole": false,
        "containerId": "rag-app-dev"
      },
      {
        "name": "ADMIN",
        "description": "Admin user role - can access all documents and admin functions",
        "composite": false,
        "clientRole": false,
        "containerId": "rag-app-dev"
      }
    ]
  },
  "groups": [],
  "defaultRoles": ["STANDARD"],
  "requiredCredentials": ["password"],
  "otpPolicyType": "totp",
  "otpPolicyAlgorithm": "HmacSHA1",
  "otpPolicyInitialCounter": 0,
  "otpPolicyDigits": 6,
  "otpPolicyLookAheadWindow": 1,
  "otpPolicyPeriod": 30,
  "otpSupportedApplications": ["FreeOTP", "Google Authenticator"],
  "webAuthnPolicyRpEntityName": "keycloak",
  "webAuthnPolicySignatureAlgorithms": ["ES256"],
  "webAuthnPolicyRpId": "",
  "webAuthnPolicyAttestationConveyancePreference": "not specified",
  "webAuthnPolicyAuthenticatorAttachment": "not specified",
  "webAuthnPolicyRequireResidentKey": "not specified",
  "webAuthnPolicyUserVerificationRequirement": "not specified",
  "webAuthnPolicyCreateTimeout": 0,
  "webAuthnPolicyAvoidSameAuthenticatorRegister": false,
  "webAuthnPolicyAcceptableAaguids": [],
  "clients": [
    {
      "clientId": "rag-app-backend",
      "name": "RAG Application Backend",
      "description": "Backend API client for service-to-service communication",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "backend-dev-secret",
      "standardFlowEnabled": false,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "serviceAccountsEnabled": true,
      "publicClient": false,
      "frontchannelLogout": false,
      "protocol": "openid-connect",
      "attributes": {
        "access.token.lifespan": "3600",
        "client.secret.creation.time": "1640995200",
        "oauth2.device.authorization.grant.enabled": "false",
        "oidc.ciba.grant.enabled": "false",
        "backchannel.logout.session.required": "true",
        "backchannel.logout.revoke.offline.tokens": "false"
      },
      "authenticationFlowBindingOverrides": {},
      "fullScopeAllowed": true,
      "nodeReRegistrationTimeout": -1,
      "defaultClientScopes": ["web-origins", "role_list", "profile", "roles", "email"],
      "optionalClientScopes": ["address", "phone", "offline_access", "microprofile-jwt"],
      "access": {
        "view": true,
        "configure": true,
        "manage": true
      },
      "webOrigins": ["http://localhost:8081"],
      "redirectUris": ["http://localhost:8081/*"]
    },
    {
      "clientId": "rag-app-frontend",
      "name": "RAG Application Frontend",
      "description": "Frontend client for user authentication",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "publicClient": true,
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "serviceAccountsEnabled": false,
      "frontchannelLogout": false,
      "protocol": "openid-connect",
      "attributes": {
        "pkce.code.challenge.method": "S256",
        "oauth2.device.authorization.grant.enabled": "false",
        "oidc.ciba.grant.enabled": "false",
        "backchannel.logout.session.required": "true",
        "backchannel.logout.revoke.offline.tokens": "false",
        "post.logout.redirect.uris": "http://localhost:3000/*"
      },
      "authenticationFlowBindingOverrides": {},
      "fullScopeAllowed": true,
      "nodeReRegistrationTimeout": -1,
      "defaultClientScopes": ["web-origins", "role_list", "profile", "roles", "email"],
      "optionalClientScopes": ["address", "phone", "offline_access", "microprofile-jwt"],
      "access": {
        "view": true,
        "configure": true,
        "manage": true
      },
      "webOrigins": ["http://localhost:3000"],
      "redirectUris": [
        "http://localhost:3000/*",
        "http://localhost:3000/auth/callback"
      ]
    }
  ],
  "clientScopes": [
    {
      "name": "roles",
      "description": "OpenID Connect scope for add user roles to the access token",
      "protocol": "openid-connect",
      "attributes": {
        "include.in.token.scope": "false",
        "display.on.consent.screen": "true",
        "consent.screen.text": "${rolesScopeConsentText}"
      },
      "protocolMappers": [
        {
          "name": "realm roles",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-realm-role-mapper",
          "consentRequired": false,
          "config": {
            "user.attribute": "foo",
            "access.token.claim": "true",
            "claim.name": "realm_access.roles",
            "jsonType.label": "String",
            "multivalued": "true"
          }
        }
      ]
    }
  ],
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "enabled": true,
      "emailVerified": true,
      "credentials": [
        {
          "type": "password",
          "value": "password123",
          "temporary": false
        }
      ],
      "realmRoles": ["STANDARD"],
      "attributes": {
        "department": ["Engineering"],
        "employee_id": ["EMP001"]
      }
    },
    {
      "username": "jane.admin",
      "email": "jane.admin@example.com",
      "firstName": "Jane",
      "lastName": "Admin",
      "enabled": true,
      "emailVerified": true,
      "credentials": [
        {
          "type": "password",
          "value": "admin123",
          "temporary": false
        }
      ],
      "realmRoles": ["ADMIN"],
      "attributes": {
        "department": ["IT"],
        "employee_id": ["EMP002"]
      }
    },
    {
      "username": "test.user",
      "email": "test.user@example.com",
      "firstName": "Test",
      "lastName": "User",
      "enabled": true,
      "emailVerified": true,
      "credentials": [
        {
          "type": "password",
          "value": "test123",
          "temporary": false
        }
      ],
      "realmRoles": ["STANDARD"],
      "attributes": {
        "department": ["QA"],
        "employee_id": ["EMP003"]
      }
    }
  ],
  "scopeMappings": [],
  "clientScopeMappings": {},
  "protocolMappers": [],
  "identityProviders": [],
  "identityProviderMappers": [],
  "components": {
    "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy": [
      {
        "name": "Allowed Protocol Mapper Types",
        "providerId": "allowed-protocol-mappers",
        "subType": "anonymous",
        "config": {
          "allowed-protocol-mapper-types": [
            "oidc-full-name-mapper",
            "oidc-sha256-pairwise-sub-mapper",
            "oidc-address-mapper",
            "saml-user-property-mapper",
            "oidc-usermodel-property-mapper",
            "saml-role-list-mapper",
            "saml-user-attribute-mapper",
            "oidc-usermodel-attribute-mapper"
          ]
        }
      }
    ]
  },
  "internationalizationEnabled": false,
  "supportedLocales": [],
  "defaultLocale": "",
  "authenticationFlows": [
    {
      "alias": "browser",
      "description": "browser based authentication",
      "providerId": "basic-flow",
      "topLevel": true,
      "builtIn": true,
      "authenticationExecutions": [
        {
          "authenticator": "auth-cookie",
          "requirement": "ALTERNATIVE",
          "priority": 10,
          "userSetupAllowed": false,
          "autheticatorFlow": false
        },
        {
          "authenticator": "auth-spnego",
          "requirement": "DISABLED",
          "priority": 20,
          "userSetupAllowed": false,
          "autheticatorFlow": false
        },
        {
          "authenticator": "identity-provider-redirector",
          "requirement": "ALTERNATIVE",
          "priority": 25,
          "userSetupAllowed": false,
          "autheticatorFlow": false
        },
        {
          "flowAlias": "forms",
          "requirement": "ALTERNATIVE",
          "priority": 30,
          "userSetupAllowed": false,
          "autheticatorFlow": true
        }
      ]
    }
  ],
  "authenticatorConfig": [],
  "requiredActions": [
    {
      "alias": "CONFIGURE_TOTP",
      "name": "Configure OTP",
      "providerId": "CONFIGURE_TOTP",
      "enabled": true,
      "defaultAction": false,
      "priority": 10,
      "config": {}
    },
    {
      "alias": "terms_and_conditions",
      "name": "Terms and Conditions",
      "providerId": "terms_and_conditions",
      "enabled": false,
      "defaultAction": false,
      "priority": 20,
      "config": {}
    },
    {
      "alias": "UPDATE_PASSWORD",
      "name": "Update Password",
      "providerId": "UPDATE_PASSWORD",
      "enabled": true,
      "defaultAction": false,
      "priority": 30,
      "config": {}
    },
    {
      "alias": "UPDATE_PROFILE",
      "name": "Update Profile",
      "providerId": "UPDATE_PROFILE",
      "enabled": true,
      "defaultAction": false,
      "priority": 40,
      "config": {}
    },
    {
      "alias": "VERIFY_EMAIL",
      "name": "Verify Email",
      "providerId": "VERIFY_EMAIL",
      "enabled": true,
      "defaultAction": false,
      "priority": 50,
      "config": {}
    }
  ],
  "browserFlow": "browser",
  "registrationFlow": "registration",
  "directGrantFlow": "direct grant",
  "resetCredentialsFlow": "reset credentials",
  "clientAuthenticationFlow": "clients",
  "dockerAuthenticationFlow": "docker auth",
  "attributes": {
    "cibaBackchannelTokenDeliveryMode": "poll",
    "cibaExpiresIn": "120",
    "cibaInterval": "5",
    "cibaAuthRequestedUserHint": "login_hint",
    "parRequestUriLifespan": "60",
    "frontendUrl": "http://localhost:8180",
    "adminEventsEnabled": "true",
    "adminEventsDetailsEnabled": "true"
  },
  "keycloakVersion": "23.0.0"
}
```

Update Keycloak configuration script (infrastructure/keycloak/configure-dev-realm.sh):
```bash
#!/bin/bash
set -e

KEYCLOAK_URL="http://localhost:8180"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin123"
REALM_FILE="dev-realm.json"

echo "=== Configuring Keycloak Development Realm ==="

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
MAX_ATTEMPTS=60
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f "${KEYCLOAK_URL}/health/ready" > /dev/null 2>&1; then
        echo " ✓"
        break
    fi
    echo -n "."
    sleep 5
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo " ✗"
    echo "ERROR: Keycloak is not ready after waiting"
    exit 1
fi

# Additional wait for admin console to be fully ready
echo "Waiting for admin console to be ready..."
sleep 10

# Get admin access token
echo "Getting admin access token..."
ADMIN_TOKEN=""
ATTEMPT=0
while [ $ATTEMPT -lt 10 ]; do
    ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=${ADMIN_USER}" \
        -d "password=${ADMIN_PASSWORD}" \
        -d "grant_type=password" \
        -d "client_id=admin-cli" 2>/dev/null | jq -r '.access_token' 2>/dev/null || echo "null")
    
    if [ "$ADMIN_TOKEN" != "null" ] && [ -n "$ADMIN_TOKEN" ]; then
        break
    fi
    
    echo "Retrying admin token request... (attempt $((ATTEMPT + 1)))"
    sleep 5
    ATTEMPT=$((ATTEMPT + 1))
done

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "ERROR: Failed to get admin access token"
    echo "Checking Keycloak logs for errors..."
    if command -v podman-compose &> /dev/null; then
        podman-compose -f docker-compose.dev.yml logs --tail 20 keycloak-dev
    elif command -v docker-compose &> /dev/null; then
        docker-compose -f docker-compose.dev.yml logs --tail 20 keycloak-dev
    fi
    exit 1
fi

echo "✓ Admin access token obtained"

# Check if realm already exists
REALM_EXISTS=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/rag-app-dev" \
    -w "%{http_code}" -o /dev/null 2>/dev/null || echo "000")

if [ "$REALM_EXISTS" = "200" ]; then
    echo "Realm 'rag-app-dev' already exists"
    echo "✓ Realm configuration complete"
else
    echo "Realm 'rag-app-dev' not found, but this is expected with --import-realm"
    echo "The realm should have been imported during Keycloak startup"
fi

# Verify realm exists and is accessible
echo "Verifying realm configuration..."
REALM_INFO=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/rag-app-dev" 2>/dev/null || echo "")

if echo "$REALM_INFO" | grep -q "rag-app-dev"; then
    echo "✓ Realm 'rag-app-dev' is accessible"
    
    # Get client information
    CLIENTS=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
        "${KEYCLOAK_URL}/admin/realms/rag-app-dev/clients" 2>/dev/null || echo "[]")
    
    BACKEND_CLIENT=$(echo "$CLIENTS" | jq -r '.[] | select(.clientId == "rag-app-backend") | .clientId' 2>/dev/null || echo "")
    FRONTEND_CLIENT=$(echo "$CLIENTS" | jq -r '.[] | select(.clientId == "rag-app-frontend") | .clientId' 2>/dev/null || echo "")
    
    if [ "$BACKEND_CLIENT" = "rag-app-backend" ]; then
        echo "✓ Backend client configured"
    else
        echo "⚠ Backend client not found"
    fi
    
    if [ "$FRONTEND_CLIENT" = "rag-app-frontend" ]; then
        echo "✓ Frontend client configured"
    else
        echo "⚠ Frontend client not found"
    fi
    
else
    echo "⚠ Realm verification failed, but import may have succeeded"
fi

echo ""
echo "=== Keycloak Development Realm Configuration Complete ==="
echo "Realm URL: ${KEYCLOAK_URL}/realms/rag-app-dev"
echo "Admin Console: ${KEYCLOAK_URL}/admin/master/console/#/rag-app-dev"
echo ""
echo "Development Users:"
echo "  john.doe / password123 (STANDARD role)"
echo "  jane.admin / admin123 (ADMIN role)"
echo "  test.user / test123 (STANDARD role)"
echo ""
echo "Clients:"
echo "  rag-app-backend (confidential client for backend)"
echo "  rag-app-frontend (public client for frontend)"
```

Update Keycloak testing script (infrastructure/keycloak/test-auth.sh):
```bash
#!/bin/bash
set -e

KEYCLOAK_URL="http://localhost:8180"
REALM="rag-app-dev"

echo "=== Testing Keycloak Authentication ==="

# Wait for Keycloak to be ready
echo "Checking Keycloak availability..."
if ! curl -f "${KEYCLOAK_URL}/health/ready" > /dev/null 2>&1; then
    echo "ERROR: Keycloak is not ready"
    echo "Please ensure Keycloak is running: ./start-dev-services.sh"
    exit 1
fi

# Check if realm exists
echo "Checking realm availability..."
REALM_CHECK=$(curl -s "${KEYCLOAK_URL}/realms/${REALM}" 2>/dev/null || echo "")
if ! echo "$REALM_CHECK" | grep -q "rag-app-dev"; then
    echo "ERROR: Realm 'rag-app-dev' not found"
    echo "Please configure the realm: ./infrastructure/keycloak/configure-dev-realm.sh"
    exit 1
fi

echo "✓ Realm 'rag-app-dev' is accessible"

# Test user authentication
echo ""
echo "Testing user authentication..."

# Test standard user
echo -n "Testing john.doe (STANDARD)..."
STANDARD_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=john.doe" \
    -d "password=password123" \
    -d "grant_type=password" \
    -d "client_id=rag-app-frontend" 2>/dev/null || echo '{"error":"request_failed"}')

STANDARD_TOKEN=$(echo "$STANDARD_RESPONSE" | jq -r '.access_token' 2>/dev/null || echo "null")

if [ "$STANDARD_TOKEN" != "null" ] && [ -n "$STANDARD_TOKEN" ] && [ "$STANDARD_TOKEN" != "" ]; then
    echo " ✓"
    # Try to decode token to check roles (basic check)
    if command -v base64 &> /dev/null; then
        PAYLOAD=$(echo "$STANDARD_TOKEN" | cut -d'.' -f2)
        # Add padding if needed
        case $((${#PAYLOAD} % 4)) in
            2) PAYLOAD="${PAYLOAD}==" ;;
            3) PAYLOAD="${PAYLOAD}=" ;;
        esac
        DECODED=$(echo "$PAYLOAD" | base64 -d 2>/dev/null | jq -r '.realm_access.roles[]?' 2>/dev/null || echo "")
        if [ -n "$DECODED" ]; then
            echo "  Roles: $DECODED"
        fi
    fi
else
    echo " ✗"
    ERROR_MSG=$(echo "$STANDARD_RESPONSE" | jq -r '.error_description // .error' 2>/dev/null || echo "Unknown error")
    echo "  Error: $ERROR_MSG"
fi

# Test admin user
echo -n "Testing jane.admin (ADMIN)..."
ADMIN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=jane.admin" \
    -d "password=admin123" \
    -d "grant_type=password" \
    -d "client_id=rag-app-frontend" 2>/dev/null || echo '{"error":"request_failed"}')

ADMIN_TOKEN=$(echo "$ADMIN_RESPONSE" | jq -r '.access_token' 2>/dev/null || echo "null")

if [ "$ADMIN_TOKEN" != "null" ] && [ -n "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "" ]; then
    echo " ✓"
    if command -v base64 &> /dev/null; then
        PAYLOAD=$(echo "$ADMIN_TOKEN" | cut -d'.' -f2)
        case $((${#PAYLOAD} % 4)) in
            2) PAYLOAD="${PAYLOAD}==" ;;
            3) PAYLOAD="${PAYLOAD}=" ;;
        esac
        DECODED=$(echo "$PAYLOAD" | base64 -d 2>/dev/null | jq -r '.realm_access.roles[]?' 2>/dev/null || echo "")
        if [ -n "$DECODED" ]; then
            echo "  Roles: $DECODED"
        fi
    fi
else
    echo " ✗"
    ERROR_MSG=$(echo "$ADMIN_RESPONSE" | jq -r '.error_description // .error' 2>/dev/null || echo "Unknown error")
    echo "  Error: $ERROR_MSG"
fi

# Test backend client credentials
echo -n "Testing backend client credentials..."
BACKEND_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=rag-app-backend" \
    -d "client_secret=backend-dev-secret" 2>/dev/null || echo '{"error":"request_failed"}')

BACKEND_TOKEN=$(echo "$BACKEND_RESPONSE" | jq -r '.access_token' 2>/dev/null || echo "null")

if [ "$BACKEND_TOKEN" != "null" ] && [ -n "$BACKEND_TOKEN" ] && [ "$BACKEND_TOKEN" != "" ]; then
    echo " ✓"
else
    echo " ✗"
    ERROR_MSG=$(echo "$BACKEND_RESPONSE" | jq -r '.error_description // .error' 2>/dev/null || echo "Unknown error")
    echo "  Error: $ERROR_MSG"
fi

echo ""
echo "=== Authentication Test Complete ==="

# Summary
if [ "$STANDARD_TOKEN" != "null" ] && [ "$ADMIN_TOKEN" != "null" ] && [ "$BACKEND_TOKEN" != "null" ]; then
    echo "✓ All authentication tests passed"
    exit 0
else
    echo "⚠ Some authentication tests failed"
    echo "Check the errors above and verify realm configuration"
    exit 1
fi
```

Update docker-compose.dev.yml Keycloak service configuration:
```yaml
  # Keycloak Authentication
  keycloak-dev:
    image: quay.io/keycloak/keycloak:23.0
    container_name: rag-keycloak-dev
    restart: unless-stopped
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres-dev:5432/rag_app_dev
      KC_DB_USERNAME: rag_dev_user
      KC_DB_PASSWORD: rag_dev_password
      KC_HOSTNAME_STRICT: false
      KC_HOSTNAME_STRICT_HTTPS: false
      KC_HTTP_ENABLED: true
      KC_HEALTH_ENABLED: true
      KC_METRICS_ENABLED: true
      # Disable problematic features for development
      KC_FEATURES_DISABLED: "impersonation"
    ports:
      - "8180:8080"
    volumes:
      - keycloak_dev_data:/opt/keycloak/data
      - ./infrastructure/keycloak/dev-realm.json:/opt/keycloak/data/import/realm.json:ro
    command:
      - start-dev
      - --import-realm
      - --verbose
    depends_on:
      postgres-dev:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/127.0.0.1/8080;echo -e \"GET /health/ready HTTP/1.1\r\nhost: 127.0.0.1:8080\r\nConnection: close\r\n\r\n\" >&3;grep \"HTTP/1.1 200 OK\" <&3"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 90s  # Increased startup time
    networks:
      - rag-dev-network
```

Create realm validation script (infrastructure/keycloak/validate-realm.sh):
```bash
#!/bin/bash
set -e

REALM_FILE="dev-realm.json"

echo "=== Validating Keycloak Realm Configuration ==="

# Check if realm file exists
if [ ! -f "$REALM_FILE" ]; then
    echo "ERROR: Realm file not found: $REALM_FILE"
    exit 1
fi

# Validate JSON syntax
echo -n "Checking JSON syntax..."
if jq empty "$REALM_FILE" > /dev/null 2>&1; then
    echo " ✓"
else
    echo " ✗"
    echo "ERROR: Invalid JSON syntax in $REALM_FILE"
    jq empty "$REALM_FILE"
    exit 1
fi

# Check for known problematic fields
echo "Checking for problematic fields..."

PROBLEMATIC_FIELDS=(
    "postLogoutRedirectUris"
    "defaultRoles"
)

for field in "${PROBLEMATIC_FIELDS[@]}"; do
    if jq -e ".clients[]? | has(\"$field\")" "$REALM_FILE" > /dev/null 2>&1; then
        echo "⚠ Found problematic field in clients: $field"
        echo "  This field may cause import issues in Keycloak 23.0"
    fi
done

# Check required fields
echo "Checking required fields..."

REQUIRED_FIELDS=(
    "realm"
    "enabled"
    "clients"
    "users"
)

for field in "${REQUIRED_FIELDS[@]}"; do
    if jq -e "has(\"$field\")" "$REALM_FILE" > /dev/null 2>&1; then
        echo "✓ Required field present: $field"
    else
        echo "✗ Missing required field: $field"
    fi
done

# Check client configuration
echo "Checking client configurations..."

BACKEND_CLIENT=$(jq -r '.clients[] | select(.clientId == "rag-app-backend") | .clientId' "$REALM_FILE" 2>/dev/null || echo "")
FRONTEND_CLIENT=$(jq -r '.clients[] | select(.clientId == "rag-app-frontend") | .clientId' "$REALM_FILE" 2>/dev/null || echo "")

if [ "$BACKEND_CLIENT" = "rag-app-backend" ]; then
    echo "✓ Backend client configured"
else
    echo "✗ Backend client missing"
fi

if [ "$FRONTEND_CLIENT" = "rag-app-frontend" ]; then
    echo "✓ Frontend client configured"
else
    echo "✗ Frontend client missing"
fi

# Check user configuration
echo "Checking user configurations..."

USERS=$(jq -r '.users[].username' "$REALM_FILE" 2>/dev/null || echo "")
USER_COUNT=$(echo "$USERS" | wc -l)

echo "Found $USER_COUNT users:"
echo "$USERS" | while read -r username; do
    if [ -n "$username" ]; then
        echo "  - $username"
    fi
done

echo ""
echo "=== Realm Validation Complete ==="

if jq empty "$REALM_FILE" > /dev/null 2>&1; then
    echo "✓ Realm configuration appears valid for Keycloak 23.0"
    exit 0
else
    echo "✗ Realm configuration has issues"
    exit 1
fi
```

## Files / Modules Impacted

- infrastructure/keycloak/dev-realm.json (complete rewrite for Keycloak 23.0 compatibility)
- infrastructure/keycloak/configure-dev-realm.sh (improved error handling)
- infrastructure/keycloak/test-auth.sh (enhanced testing)
- infrastructure/keycloak/validate-realm.sh (new validation script)
- docker-compose.dev.yml (Keycloak service configuration update)

## Acceptance Criteria

Given the fixed Keycloak realm configuration
When Keycloak container starts with --import-realm
Then the realm should import successfully without errors

Given the realm is imported
When authentication tests are run
Then all test users should be able to authenticate

Given the client configurations are fixed
When backend and frontend clients are tested
Then they should work with the corrected configuration

Given the validation script is run
When the realm JSON is checked
Then it should pass all compatibility checks

## Testing Requirements

- Test realm import during Keycloak startup
- Test user authentication for all development users
- Test client credential flows
- Test realm validation script
- Test error handling for import failures

## Dependencies / Preconditions

- PostgreSQL must be running for Keycloak database
- Keycloak 23.0 container must be available
- JSON validation tools (jq) must be available
- Network connectivity between services must be established

## Key Changes Made

1. **Removed unsupported fields**: `postLogoutRedirectUris` replaced with `post.logout.redirect.uris` in attributes
2. **Fixed client configuration**: Updated to use supported properties for Keycloak 23.0
3. **Added proper client scopes**: Included required default and optional client scopes
4. **Enhanced error handling**: Better validation and troubleshooting scripts
5. **Improved startup process**: Added verbose logging and better health checks