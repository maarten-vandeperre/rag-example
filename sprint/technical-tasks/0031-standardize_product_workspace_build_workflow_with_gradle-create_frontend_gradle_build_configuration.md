# Create Frontend Gradle Build Configuration

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Create Gradle build configuration for the React frontend module with Node.js integration, development server, testing, and build tasks.

## Scope

- Create frontend/build.gradle with Node.js plugin configuration
- Configure development server task for live reload
- Set up test execution for React components
- Configure build and packaging tasks for production
- Add npm/yarn dependency management through Gradle

## Out of Scope

- Backend integration tasks
- Docker image building through Gradle
- CDN deployment configuration
- Advanced webpack customization

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0029-standardize_product_workspace_build_workflow_with_gradle-create_root_gradle_build_configuration.md

## Implementation Details

Create frontend/build.gradle with:
- Node.js plugin application and configuration
- npm/yarn task integration
- Development server configuration
- Test execution setup
- Production build configuration

Frontend build.gradle configuration:
```gradle
plugins {
    id 'com.github.node-gradle.node'
}

node {
    version = '18.17.0'
    npmVersion = '9.6.7'
    download = true
    workDir = file("${project.projectDir}/.gradle/nodejs")
    npmWorkDir = file("${project.projectDir}/.gradle/npm")
}

// Install dependencies
npmInstall {
    inputs.file('package.json')
    inputs.file('package-lock.json')
    outputs.dir('node_modules')
}

// Development server task
task dev(type: NpmTask) {
    group = 'application'
    description = 'Starts the React development server'
    dependsOn 'npmInstall'
    npmCommand = ['start']
}

// Test execution task
task test(type: NpmTask) {
    group = 'verification'
    description = 'Runs React component tests'
    dependsOn 'npmInstall'
    npmCommand = ['test', '--', '--coverage', '--watchAll=false']
}

// Production build task
task buildProd(type: NpmTask) {
    group = 'build'
    description = 'Builds the React application for production'
    dependsOn 'npmInstall'
    npmCommand = ['run', 'build']
    inputs.dir('src')
    inputs.file('package.json')
    outputs.dir('build')
}

// Lint task
task lint(type: NpmTask) {
    group = 'verification'
    description = 'Runs ESLint on the React application'
    dependsOn 'npmInstall'
    npmCommand = ['run', 'lint']
}

// Clean task
clean {
    delete 'build'
    delete 'node_modules'
    delete '.gradle'
}

// Build task depends on production build
build {
    dependsOn 'buildProd'
}

// Verification task
task verify {
    group = 'verification'
    description = 'Runs all verification tasks'
    dependsOn 'test', 'lint'
}
```

Package.json scripts integration:
```json
{
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "lint": "eslint src --ext .js,.jsx,.ts,.tsx"
  }
}
```

Development server configuration:
- Hot module replacement enabled
- Proxy configuration for backend API
- Environment variable support
- Port configuration

## Files / Modules Impacted

- frontend/build.gradle
- frontend/package.json (script updates)
- frontend/.env.development (if needed)

## Acceptance Criteria

Given the frontend Gradle configuration is created
When ./gradlew :frontend:dev is executed
Then the React development server should start successfully

Given the test configuration is set up
When ./gradlew :frontend:test is executed
Then all React tests should run and report results

Given the build configuration is complete
When ./gradlew :frontend:build is executed
Then the frontend should build for production successfully

Given development server is running
When React code is modified
Then the browser should automatically reload

## Testing Requirements

- Test Node.js plugin installation
- Test npm dependency installation
- Test development server startup
- Test React test execution
- Test production build process

## Dependencies / Preconditions

- Root Gradle configuration must exist
- Node.js plugin must be available
- Frontend source code structure must exist
- package.json must be properly configured