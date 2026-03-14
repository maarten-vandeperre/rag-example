## Summary
Added the initial user domain model with role-based access support and validation for usernames, email addresses, and lifecycle timestamps.

## Changes
- `backend/src/main/java/com/rag/app/domain/entities/User.java`
- `backend/src/main/java/com/rag/app/domain/valueobjects/UserRole.java`
- `backend/src/test/java/com/rag/app/domain/entities/UserTest.java`
- `backend/src/test/java/com/rag/app/domain/valueobjects/UserRoleTest.java`

## Impact
The backend domain now has a validated user entity that can be used to model ownership and admin access rules for uploaded documents.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Introduce dedicated value objects for email addresses and usernames if richer normalization or uniqueness rules are needed.
- Connect the user entity to document ownership logic when application services are added.
