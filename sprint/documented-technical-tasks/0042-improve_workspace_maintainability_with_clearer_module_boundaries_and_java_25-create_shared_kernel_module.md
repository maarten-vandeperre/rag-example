# Create Shared Kernel Module

## Related User Story

User Story: improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25

## Objective

Create a shared kernel module that contains common domain concepts, value objects, and interfaces that are shared across all product area modules while maintaining clear boundaries.

## Scope

- Create shared-kernel module structure
- Define common domain value objects and primitives
- Create shared interfaces and contracts
- Establish common exceptions and error handling
- Define module communication protocols

## Out of Scope

- Business logic implementation (belongs in specific modules)
- Infrastructure concerns (belongs in individual modules)
- Framework-specific code
- Module-specific domain concepts

## Clean Architecture Placement

domain (shared concepts only)

## Execution Dependencies

- 0038-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-analyze_current_module_structure.md

## Implementation Details

Create shared-kernel module structure:
```
backend/shared-kernel/
├── src/main/java/com/rag/app/shared/
│   ├── domain/
│   │   ├── valueobjects/
│   │   │   ├── EntityId.java
│   │   │   ├── Timestamp.java
│   │   │   ├── Email.java
│   │   │   └── FileSize.java
│   │   ├── events/
│   │   │   ├── DomainEvent.java
│   │   │   └── EventPublisher.java
│   │   └── exceptions/
│   │       ├── DomainException.java
│   │       ├── ValidationException.java
│   │       └── BusinessRuleViolationException.java
│   ├── interfaces/
│   │   ├── Repository.java
│   │   ├── UseCase.java
│   │   └── ModuleFacade.java
│   └── utils/
│       ├── Validator.java
│       └── StringUtils.java
├── src/test/java/
└── build.gradle
```

Module build.gradle:
```gradle
plugins {
    id 'java-library'
}

dependencies {
    // Only fundamental dependencies allowed
    implementation 'org.slf4j:slf4j-api'
    
    // Test dependencies
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.assertj:assertj-core'
}

// Enforce no dependencies on other application modules
configurations.all {
    resolutionStrategy.eachDependency { details ->
        if (details.requested.group == 'com.rag.app' && 
            details.requested.name != 'shared-kernel') {
            throw new GradleException(
                "Shared kernel cannot depend on other application modules: ${details.requested}"
            )
        }
    }
}
```

Common value objects:
```java
// Base class for entity identifiers
public abstract class EntityId {
    private final String value;
    
    protected EntityId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Entity ID cannot be null or empty");
        }
        this.value = value.trim();
    }
    
    public String getValue() { return value; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EntityId entityId = (EntityId) obj;
        return Objects.equals(value, entityId.value);
    }
    
    @Override
    public int hashCode() { return Objects.hash(value); }
    
    @Override
    public String toString() { return value; }
}

// Common timestamp value object
public class Timestamp {
    private final Instant instant;
    
    private Timestamp(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "Instant cannot be null");
    }
    
    public static Timestamp now() {
        return new Timestamp(Instant.now());
    }
    
    public static Timestamp of(Instant instant) {
        return new Timestamp(instant);
    }
    
    public Instant getInstant() { return instant; }
    public boolean isBefore(Timestamp other) { return instant.isBefore(other.instant); }
    public boolean isAfter(Timestamp other) { return instant.isAfter(other.instant); }
}

// Email value object
public class Email {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    private final String value;
    
    public Email(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException("Invalid email format: " + value);
        }
        this.value = value.toLowerCase();
    }
    
    public String getValue() { return value; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Email email = (Email) obj;
        return Objects.equals(value, email.value);
    }
    
    @Override
    public int hashCode() { return Objects.hash(value); }
    
    @Override
    public String toString() { return value; }
}

// File size value object
public class FileSize {
    private static final long MAX_FILE_SIZE = 41_943_040L; // 40MB
    
    private final long bytes;
    
    public FileSize(long bytes) {
        if (bytes < 0) {
            throw new ValidationException("File size cannot be negative");
        }
        if (bytes > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds maximum allowed size of 40MB");
        }
        this.bytes = bytes;
    }
    
    public long getBytes() { return bytes; }
    public double getMegabytes() { return bytes / 1_048_576.0; }
    public boolean isWithinLimit() { return bytes <= MAX_FILE_SIZE; }
}
```

Common interfaces:
```java
// Base repository interface
public interface Repository<T, ID> {
    void save(T entity);
    Optional<T> findById(ID id);
    void delete(ID id);
    boolean exists(ID id);
}

// Base use case interface
public interface UseCase<INPUT, OUTPUT> {
    OUTPUT execute(INPUT input);
}

// Module facade interface
public interface ModuleFacade {
    String getModuleName();
    String getModuleVersion();
    boolean isHealthy();
}
```

Domain events:
```java
// Base domain event
public abstract class DomainEvent {
    private final String eventId;
    private final Timestamp occurredAt;
    private final String eventType;
    
    protected DomainEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Timestamp.now();
        this.eventType = eventType;
    }
    
    public String getEventId() { return eventId; }
    public Timestamp getOccurredAt() { return occurredAt; }
    public String getEventType() { return eventType; }
}

// Event publisher interface
public interface EventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
```

Common exceptions:
```java
// Base domain exception
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    
    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() { return errorCode; }
}

// Validation exception
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}

// Business rule violation exception
public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String rule, String message) {
        super("BUSINESS_RULE_VIOLATION", String.format("Rule '%s' violated: %s", rule, message));
    }
}
```

Utility classes:
```java
// Common validator
public class Validator {
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
    }
    
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be null or empty");
        }
    }
    
    public static void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }
}
```

## Files / Modules Impacted

- backend/shared-kernel/build.gradle
- backend/shared-kernel/src/main/java/com/rag/app/shared/domain/valueobjects/EntityId.java
- backend/shared-kernel/src/main/java/com/rag/app/shared/domain/valueobjects/Timestamp.java
- backend/shared-kernel/src/main/java/com/rag/app/shared/domain/valueobjects/Email.java
- backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/Repository.java
- backend/shared-kernel/src/main/java/com/rag/app/shared/domain/exceptions/DomainException.java
- backend/settings.gradle (add module)

## Acceptance Criteria

Given the shared kernel module is created
When the module is built independently
Then it should compile and test successfully without any other dependencies

Given common value objects are defined
When other modules use them
Then they should provide consistent validation and behavior

Given common interfaces are defined
When modules implement them
Then they should follow consistent contracts

Given module boundaries are enforced
When attempting to add dependencies to other modules
Then the build should fail with clear error messages

## Testing Requirements

- Unit tests for all value objects and validation
- Tests for common interfaces and contracts
- Module boundary enforcement tests
- Exception handling tests
- Utility class tests

## Dependencies / Preconditions

- Understanding of Domain-Driven Design concepts
- Knowledge of shared kernel patterns
- Clean Architecture principles
- Analysis of common concepts across modules