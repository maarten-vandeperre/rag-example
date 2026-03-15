# Standardize local development environment with Podman

## User Story

As an internal developer  
I want to bootstrap the product workspace's local development environment through a Podman-based shared setup for supporting services  
So that I can work in a more reliable and secure local environment, onboard faster, and continue running the backend and front-end separately in development mode.

## Context

Internal developers need a simpler and more reliable way to prepare the local development environment across the whole product workspace. The local setup should provide the supporting services required for development while allowing the backend and front-end to keep running separately in their normal development workflows. This change is intended to improve reliability, security, onboarding, and ease of daily local development. If the local environment cannot be started successfully, the issue must be clear enough to escalate to the lead architect.

## Screens / User Journey

Screen: Developer local environment setup

User action:  
The developer opens the product workspace and starts the shared local development environment.

Expected result:  
The supporting services required for development are started and available for use.

Screen: Developer application startup workflow

User action:  
After the supporting services are running, the developer starts the backend and front-end separately in development mode.

Expected result:  
The backend and front-end run next to the shared supporting services without being included in the shared environment startup.

Screen: Developer daily development workflow

User action:  
The developer uses the shared local environment during normal daily development work.

Expected result:  
The developer can use the application with the required supporting services in a reliable local setup.

Screen: Developer onboarding workflow

User action:  
A developer new to the project prepares the local development environment.

Expected result:  
The local setup is easier to understand and complete than before.

Screen: Local environment failure handling

User action:  
The developer attempts to start the local environment and it does not complete successfully.

Expected result:  
The reason for failure is visible and can be escalated to the lead architect.

## Functional Requirements

- Internal developers must be able to start a shared local development environment for the whole product workspace using Podman.
- The shared local development environment must include supporting services needed for development.
- Supporting services must include at least the vector database and Keycloak, with room for additional required supporting services.
- The backend must remain started separately by developers in development mode and not as part of the shared environment startup.
- The front-end must remain started separately by developers in development mode and not as part of the shared environment startup.
- Developers must be able to use the shared supporting services together with separately started backend and front-end workflows.
- The local setup must support easier onboarding and daily development.
- If the local environment fails to start, the reason for failure must be clear enough to support escalation to the lead architect.

## Acceptance Criteria

Given an internal developer needs a local development environment  
When the developer starts the shared Podman-based setup  
Then the required supporting services should become available for development use.

Given the shared local environment is running  
When the developer starts the backend separately in development mode  
Then the backend should run successfully next to the shared supporting services.

Given the shared local environment is running  
When the developer starts the front-end separately in development mode  
Then the front-end should run successfully next to the shared supporting services.

Given an internal developer is performing daily development tasks  
When the developer uses the local environment  
Then the supporting services should be reliably available throughout normal development work.

Given a new internal developer joins the project  
When the developer prepares the local development environment  
Then the setup should be easier to understand and complete.

Given the local development environment requires supporting services  
When the shared setup is started  
Then the vector database and Keycloak should be included among the available services.

Given the local environment cannot be started successfully  
When the failure occurs  
Then the reason for the failure should be visible and understandable for escalation to the lead architect.

## Functional Test Scenarios

### Test: Start supporting services for local development

Steps:

1. Open the full product workspace.
2. Start the shared local development environment.
3. Wait for the supporting services to become available.

Expected result:

- The shared local environment starts successfully.
- Required supporting services are available.
- The environment is ready for local development use.

### Test: Start backend next to shared local environment

Steps:

1. Start the shared local development environment.
2. Start the backend separately in development mode.
3. Validate that the backend can work with the available supporting services.

Expected result:

- The backend starts separately from the shared environment.
- The backend works correctly with the running supporting services.

### Test: Start front-end next to shared local environment

Steps:

1. Start the shared local development environment.
2. Start the front-end separately in development mode.
3. Validate that the front-end can be used in the local development setup.

Expected result:

- The front-end starts separately from the shared environment.
- The front-end works correctly alongside the shared supporting services.

### Test: Onboard a developer into the local setup

Steps:

1. Use the product workspace from the perspective of a developer new to the project.
2. Follow the local setup workflow.
3. Start the supporting services and then start the backend and front-end separately.

Expected result:

- The onboarding flow is easy to understand.
- The developer can complete local setup with less friction.
- The local development environment is usable after setup.

### Test: Handle local environment startup failure

Steps:

1. Trigger a condition where the shared local environment cannot start successfully.
2. Review the failure information.

Expected result:

- The startup failure is clearly indicated.
- The reason for failure is visible.
- The information is sufficient to escalate the issue to the lead architect.

## Edge Cases

- The shared local environment starts, but one or more required supporting services are unavailable.
- The backend or front-end cannot run correctly next to the shared supporting services.
- The local environment works for some developers but not others, reducing onboarding consistency.
- A supporting service required by the workspace is missing from the shared local environment.
- The local environment starts, but daily development remains unreliable or difficult.
- Failure information is too unclear to support escalation to the lead architect.
