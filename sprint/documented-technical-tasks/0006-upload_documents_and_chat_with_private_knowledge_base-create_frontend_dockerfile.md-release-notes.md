## Summary
Added the missing frontend container assets and a minimal React application so the compose frontend service can build, serve production assets with nginx, and proxy API traffic to the backend.

## Changes
Created `frontend/Dockerfile`, `frontend/.dockerignore`, `frontend/nginx.conf`, `frontend/.env.production`, `frontend/package.json`, `frontend/package-lock.json`, `frontend/.gitignore`, `frontend/public/index.html`, `frontend/src/App.js`, `frontend/src/App.css`, `frontend/src/App.test.js`, `frontend/src/index.js`, and `frontend/src/setupTests.js`.
Updated `docker-compose.yml` to align frontend container environment variables with the nginx `/api` proxy setup.

## Impact
The repository now contains a buildable frontend image for the private knowledge base UI, with static asset caching, React Router fallback handling, non-root nginx runtime, and container-friendly API routing.

## Verification
Executed `npm install`, `npm test -- --watchAll=false`, `npm run build`, `podman build -t rag-example-frontend:test .`, `podman run -d --add-host backend:127.0.0.1 -p 3001:3000 localhost/rag-example-frontend:test`, `curl -I http://127.0.0.1:3001/`, `curl -I http://127.0.0.1:3001/static/js/main.17cd62e3.js`, `curl -I http://127.0.0.1:3001/chat/session`, `podman network create rag-example-frontend-test`, `podman run -d --network rag-example-frontend-test --network-alias backend docker.io/hashicorp/http-echo:1.0.0 -listen=:8080 -text='api ok'`, `podman run -d --network rag-example-frontend-test -p 3002:3000 localhost/rag-example-frontend:test`, `curl -i http://127.0.0.1:3002/api/`, and `mvn -gs maven-settings.xml -s maven-settings.xml test`.

## Follow-ups
Replace the placeholder React shell with the actual document upload and chat workflows as later frontend tasks are implemented.
