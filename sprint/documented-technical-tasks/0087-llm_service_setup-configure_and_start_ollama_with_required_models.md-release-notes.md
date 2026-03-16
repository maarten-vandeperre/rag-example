## Summary
Configured the backend to use a real Ollama client in development, ensured the Ollama service starts automatically with dev services, and verified the required `tinyllama` model is pulled and usable for chat responses.

## Changes
- Added `backend/src/main/java/com/rag/app/config/LlmConfiguration.java`
- Added `backend/src/main/java/com/rag/app/infrastructure/llm/OllamaLlmClient.java`
- Updated `backend/src/main/java/com/rag/app/infrastructure/llm/HeuristicLlmClient.java`
- Updated `backend/src/main/java/com/rag/app/config/DevelopmentConfiguration.java`
- Updated `backend/src/main/resources/application-dev.properties`
- Added `backend/src/test/java/com/rag/app/infrastructure/llm/OllamaLlmClientTest.java`
- Updated `infrastructure/ollama/init-ollama.sh`
- Updated `start-dev-services.sh`
- Updated `.env.dev`

## Impact
Development environments now bring up Ollama automatically, guarantee `tinyllama` availability, and route backend LLM calls through Ollama with configured timeout and retry behavior instead of relying only on the heuristic in-memory implementation.

## Verification
- `./gradlew :backend:compileJava`
- `./gradlew :backend:test --tests com.rag.app.infrastructure.llm.OllamaLlmClientTest`
- `./start-dev-services.sh`
- `curl http://localhost:11434/api/tags`
- `curl http://localhost:11434/api/generate -H "Content-Type: application/json" -d '{"model":"tinyllama","prompt":"Say hello in five words or fewer.","stream":false}'`
- `./gradlew :backend:quarkusDev`
- `curl http://localhost:8081/q/health`
- `curl -F "userId=11111111-1111-1111-1111-111111111111" -F "file=@/tmp/0087-chat.txt;type=text/plain" http://localhost:8081/api/documents/upload`
- `curl -X POST -H "Content-Type: application/json" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -d '{"question":"How do I upload a document?","maxResponseTimeMs":60000}' http://localhost:8081/api/chat/query`
- `./gradlew :backend:test`
- `./gradlew build`

## Follow-ups
- Consider adding a dedicated integration test profile that exercises the real Ollama endpoint without requiring manual dev-service startup.
