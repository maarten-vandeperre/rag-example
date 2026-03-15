## Summary
Fixed the Java 25 Quarkus backend compatibility path by aligning Quarkus versions across Gradle and Maven, updating backend CDI wiring for dev mode, and removing Gradle 9 Groovy DSL issues that blocked backend startup.

## Changes
Updated `backend/build.gradle` to use Gradle 9-compatible test logging assignment syntax.
Updated `backend/pom.xml` to align the Maven Quarkus platform and REST artifacts with the Java 25-compatible Quarkus 3.32.3 stack.
Updated `backend/src/main/java/com/rag/app/usecases/GetUserDocuments.java`, `backend/src/main/java/com/rag/app/usecases/GetAdminProgress.java`, and `backend/src/main/java/com/rag/app/usecases/QueryDocuments.java` with CDI bean annotations and an injectable Java 25-friendly constructor path for dev mode.

## Impact
The backend no longer fails with the prior Java 25/Quarkus startup incompatibility path, and Quarkus development mode now starts successfully on the current toolchain.

## Verification
Executed `./gradlew --no-daemon :backend:test`.
Executed `./gradlew --no-daemon -Dquarkus.http.port=8181 :backend:quarkusDev` and verified startup to the `Installed features` log line.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Clean up the remaining Quarkus dev warnings for deprecated or unrecognized config keys in `backend/src/main/resources/application*.properties` so the new dev-mode startup is also warning-free.
