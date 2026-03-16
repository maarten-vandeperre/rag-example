## Summary
Fixed the dev-environment chat failure by migrating the PostgreSQL schema so chat answer persistence can store document references and answer source references successfully.

## Changes
- Added `infrastructure/database/migrate-dev.sql`
- Updated `infrastructure/database/init-dev.sql`
- Updated `start-dev-services.sh`

## Impact
Chat queries in local development no longer fail with `Unable to process chat query` after document upload because the database now contains the columns and tables required by `JdbcAnswerPersistence`.

## Verification
- `podman exec -i rag-dev_postgres-dev_1 psql -U rag_dev_user -d rag_app_dev < infrastructure/database/migrate-dev.sql`
- `./start-dev-services.sh`
- `./gradlew :backend:quarkusDev`
- `curl http://localhost:8081/q/health`
- `curl -F "userId=11111111-1111-1111-1111-111111111111" -F "file=@/tmp/0085-chat-verify.txt;type=text/plain" http://localhost:8081/api/documents/upload`
- `curl -X POST -H "Content-Type: application/json" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -d '{"question":"How do I upload a file?","maxResponseTimeMs":20000}' http://localhost:8081/api/chat/query`
- `./gradlew :backend:compileJava`
- `./gradlew :backend:test`
- `./gradlew build`

## Follow-ups
- Consider adding an automated startup/integration regression test that validates the development database schema required by chat persistence.
