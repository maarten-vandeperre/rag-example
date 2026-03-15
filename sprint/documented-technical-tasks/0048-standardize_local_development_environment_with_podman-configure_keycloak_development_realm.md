# Configure Keycloak Development Realm

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Configure Keycloak with a development realm, clients, users, and roles specifically for local development, providing authentication and authorization services for the RAG application.

## Scope

- Create development realm configuration for Keycloak
- Configure application client for backend API access
- Configure frontend client for user authentication
- Set up development users with different roles
- Configure role mappings and permissions
- Create realm import/export functionality

## Out of Scope

- Production Keycloak configuration
- Advanced authentication flows
- External identity provider integration
- Custom Keycloak themes or extensions

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0047-standardize_local_development_environment_with_podman-create_development_services_compose.md

## Implementation Details

Create Keycloak realm configuration (infrastructure/keycloak/dev-realm.json):
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
  "roles": {
    "realm": [
      {
        "name": "STANDARD",
        "description": "Standard user role - can access own documents"
      },
      {
        "name": "ADMIN",
        "description": "Admin user role - can access all documents and admin functions"
      }
    ]
  },
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
      "protocol": "openid-connect",
      "attributes": {
        "access.token.lifespan": "3600",
        "client.secret.creation.time": "1640995200"
      },
      "defaultRoles": ["STANDARD"],
      "optionalClientScopes": ["profile", "email"],
      "webOrigins": ["http://localhost:8081"],
      "redirectUris": ["http://localhost:8081/*"]
    },
    {
      "clientId": "rag-app-frontend",
      "name": "RAG Application Frontend",
      "description": "Frontend client for user authentication",
      "enabled": true,
      "publicClient": true,
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "protocol": "openid-connect",
      "attributes": {
        "pkce.code.challenge.method": "S256"
      },
      "webOrigins": ["http://localhost:3000"],
      "redirectUris": [
        "http://localhost:3000/*",
        "http://localhost:3000/auth/callback"
      ],
      "postLogoutRedirectUris": ["http://localhost:3000/"],
      "defaultClientScopes": ["profile", "email", "roles"],
      "optionalClientScopes": ["address", "phone"]
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
  "clientScopeMappings": {
    "rag-app-backend": [
      {
        "client": "rag-app-backend",
        "roles": ["STANDARD", "ADMIN"]
      }
    ]
  },
  "scopeMappings": [
    {
      "client": "rag-app-frontend",
      "roles": ["STANDARD", "ADMIN"]
    }
  ],
  "protocolMappers": [
    {
      "name": "role-mapper",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-usermodel-realm-role-mapper",
      "consentRequired": false,
      "config": {
        "user.attribute": "role",
        "access.token.claim": "true",
        "claim.name": "roles",
        "jsonType.label": "String",
        "multivalued": "true"
      }
    },
    {
      "name": "employee-id-mapper",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-usermodel-attribute-mapper",
      "consentRequired": false,
      "config": {
        "user.attribute": "employee_id",
        "access.token.claim": "true",
        "claim.name": "employee_id",
        "jsonType.label": "String"
      }
    }
  ],
  "browserFlow": "browser",
  "registrationFlow": "registration",
  "directGrantFlow": "direct grant",
  "resetCredentialsFlow": "reset credentials",
  "clientAuthenticationFlow": "clients",
  "dockerAuthenticationFlow": "docker auth",
  "attributes": {
    "frontendUrl": "http://localhost:8180",
    "adminEventsEnabled": "true",
    "adminEventsDetailsEnabled": "true"
  }
}
```

Create Keycloak configuration script (infrastructure/keycloak/configure-dev-realm.sh):
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
until curl -f "${KEYCLOAK_URL}/health/ready" > /dev/null 2>&1; do
    echo -n "."
    sleep 5
done
echo " ✓"

# Get admin access token
echo "Getting admin access token..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=${ADMIN_USER}" \
    -d "password=${ADMIN_PASSWORD}" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "ERROR: Failed to get admin access token"
    exit 1
fi

echo "✓ Admin access token obtained"

# Check if realm already exists
REALM_EXISTS=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/rag-app-dev" \
    -w "%{http_code}" -o /dev/null)

if [ "$REALM_EXISTS" = "200" ]; then
    echo "Realm 'rag-app-dev' already exists, updating..."
    # Update existing realm
    curl -s -X PUT "${KEYCLOAK_URL}/admin/realms/rag-app-dev" \
        -H "Authorization: Bearer ${ADMIN_TOKEN}" \
        -H "Content-Type: application/json" \
        -d @"${REALM_FILE}"
    echo "✓ Realm updated"
else
    echo "Creating new realm 'rag-app-dev'..."
    # Create new realm
    curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
        -H "Authorization: Bearer ${ADMIN_TOKEN}" \
        -H "Content-Type: application/json" \
        -d @"${REALM_FILE}"
    echo "✓ Realm created"
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

Create Keycloak testing script (infrastructure/keycloak/test-auth.sh):
```bash
#!/bin/bash
set -e

KEYCLOAK_URL="http://localhost:8180"
REALM="rag-app-dev"

echo "=== Testing Keycloak Authentication ==="

# Test user authentication
echo "Testing user authentication..."

# Test standard user
echo -n "Testing john.doe (STANDARD)..."
STANDARD_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=john.doe" \
    -d "password=password123" \
    -d "grant_type=password" \
    -d "client_id=rag-app-frontend" | jq -r '.access_token')

if [ "$STANDARD_TOKEN" != "null" ] && [ -n "$STANDARD_TOKEN" ]; then
    echo " ✓"
    # Decode token to check roles
    ROLES=$(echo "$STANDARD_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq -r '.realm_access.roles[]' 2>/dev/null || echo "")
    echo "  Roles: $ROLES"
else
    echo " ✗"
fi

# Test admin user
echo -n "Testing jane.admin (ADMIN)..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=jane.admin" \
    -d "password=admin123" \
    -d "grant_type=password" \
    -d "client_id=rag-app-frontend" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" != "null" ] && [ -n "$ADMIN_TOKEN" ]; then
    echo " ✓"
    ROLES=$(echo "$ADMIN_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq -r '.realm_access.roles[]' 2>/dev/null || echo "")
    echo "  Roles: $ROLES"
else
    echo " ✗"
fi

# Test backend client credentials
echo -n "Testing backend client credentials..."
BACKEND_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=rag-app-backend" \
    -d "client_secret=backend-dev-secret" | jq -r '.access_token')

if [ "$BACKEND_TOKEN" != "null" ] && [ -n "$BACKEND_TOKEN" ]; then
    echo " ✓"
else
    echo " ✗"
fi

echo ""
echo "=== Authentication Test Complete ==="
```

Create application configuration for Keycloak integration:
```properties
# backend/src/main/resources/application-dev.properties
# Keycloak Configuration for Development
quarkus.oidc.auth-server-url=http://localhost:8180/realms/rag-app-dev
quarkus.oidc.client-id=rag-app-backend
quarkus.oidc.credentials.secret=backend-dev-secret
quarkus.oidc.tls.verification=none

# Security Configuration
quarkus.http.auth.permission.authenticated.paths=/api/*
quarkus.http.auth.permission.authenticated.policy=authenticated
quarkus.http.auth.permission.admin.paths=/api/admin/*
quarkus.http.auth.permission.admin.policy=role-admin

# CORS Configuration for Development
quarkus.http.cors=true
quarkus.http.cors.origins=http://localhost:3000
quarkus.http.cors.headers=accept,authorization,content-type,x-requested-with
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS

# Role Mapping
quarkus.security.users.embedded.enabled=false
mp.jwt.verify.publickey.location=http://localhost:8180/realms/rag-app-dev/protocol/openid-connect/certs
```

Frontend Keycloak configuration (frontend/src/config/keycloak.js):
```javascript
import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: 'http://localhost:8180',
  realm: 'rag-app-dev',
  clientId: 'rag-app-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

export const initKeycloak = async () => {
  try {
    const authenticated = await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256',
    });
    
    if (authenticated) {
      console.log('User authenticated');
      return keycloak;
    } else {
      console.log('User not authenticated');
      return keycloak;
    }
  } catch (error) {
    console.error('Keycloak initialization failed:', error);
    throw error;
  }
};

export default keycloak;
```

## Files / Modules Impacted

- infrastructure/keycloak/dev-realm.json
- infrastructure/keycloak/configure-dev-realm.sh
- infrastructure/keycloak/test-auth.sh
- backend/src/main/resources/application-dev.properties
- frontend/src/config/keycloak.js
- frontend/public/silent-check-sso.html

## Acceptance Criteria

Given Keycloak is running in development
When the realm configuration is imported
Then the rag-app-dev realm should be created with all clients and users

Given development users are configured
When authentication is tested
Then john.doe, jane.admin, and test.user should be able to authenticate

Given client configurations are set up
When backend and frontend clients are tested
Then they should be able to obtain valid tokens

Given role mappings are configured
When tokens are decoded
Then they should contain the correct user roles

## Testing Requirements

- Test realm import and configuration
- Test user authentication for all development users
- Test client credential flows
- Test role-based access control
- Test token validation and role extraction

## Dependencies / Preconditions

- Keycloak container must be running
- PostgreSQL must be available for Keycloak database
- curl and jq must be available for testing scripts
- Network connectivity between services must be established