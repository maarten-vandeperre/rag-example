# Create Frontend Dockerfile

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create Dockerfile for the React frontend application with proper Node.js runtime, build process, and production serving configuration.

## Scope

- Create multi-stage Dockerfile for React application
- Configure Node.js build environment
- Set up production serving with nginx
- Configure environment variables and API endpoints

## Out of Scope

- Advanced nginx configuration
- SSL/TLS termination
- CDN integration
- Advanced caching strategies

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_podman_compose_configuration.md

## Implementation Details

Create frontend Dockerfile with:

1. **Multi-stage build Dockerfile**:
```dockerfile
# Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci --only=production

# Copy source code and build
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built application
COPY --from=build /app/build .

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nextjs -u 1001

# Set permissions
RUN chown -R nextjs:nodejs /usr/share/nginx/html && \
    chown -R nextjs:nodejs /var/cache/nginx && \
    chown -R nextjs:nodejs /var/log/nginx && \
    chown -R nextjs:nodejs /etc/nginx/conf.d

# Switch to non-root user
USER nextjs

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:3000 || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
```

2. **Nginx configuration** (nginx.conf):
```nginx
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       3000;
        server_name  localhost;
        root         /usr/share/nginx/html;
        index        index.html;

        # Handle React Router
        location / {
            try_files $uri $uri/ /index.html;
        }

        # API proxy to backend
        location /api/ {
            proxy_pass http://backend:8080/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Static assets caching
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;
    }
}
```

3. **Environment configuration** (.env.production):
```
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_MAX_FILE_SIZE=41943040
REACT_APP_SUPPORTED_FILE_TYPES=pdf,md,txt
REACT_APP_CHAT_TIMEOUT=20000
```

4. **Build configuration** (package.json scripts):
```json
{
  "scripts": {
    "build": "react-scripts build",
    "build:docker": "REACT_APP_API_URL=/api npm run build"
  }
}
```

5. **Docker ignore file** (.dockerignore):
```
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*
.git/
.gitignore
README.md
*.md
.env.local
.env.development.local
.env.test.local
.env.production.local
docker-compose.yml
Dockerfile
```

6. **Environment variables for container**:
- REACT_APP_API_URL=/api (proxied through nginx)
- REACT_APP_MAX_FILE_SIZE=41943040
- REACT_APP_SUPPORTED_FILE_TYPES=pdf,md,txt

## Files / Modules Impacted

- frontend/Dockerfile
- frontend/.dockerignore
- frontend/nginx.conf
- frontend/.env.production
- docker-compose.yml (frontend service configuration)

## Acceptance Criteria

Given frontend Dockerfile is built
When container is started
Then React application should be served on port 3000

Given nginx configuration is applied
When API requests are made
Then requests should be proxied to backend service

Given static assets are requested
When files are served
Then appropriate caching headers should be set

Given React Router is used
When direct URLs are accessed
Then application should serve index.html for client-side routing

## Testing Requirements

- Test Docker image build process
- Test nginx configuration and serving
- Test API proxy functionality
- Test static asset serving and caching
- Test React Router compatibility

## Dependencies / Preconditions

- Podman Compose configuration must exist
- Node.js 18 base image must be available
- npm dependencies must be resolvable
- Backend service must be available for API proxy