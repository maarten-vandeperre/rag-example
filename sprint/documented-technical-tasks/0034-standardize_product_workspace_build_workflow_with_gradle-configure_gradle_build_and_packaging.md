# Configure Gradle Build and Packaging

## Related User Story

User Story: standardize_product_workspace_build_workflow_with_gradle

## Objective

Configure comprehensive build and packaging tasks through Gradle that produce release-ready outputs for both backend and frontend modules.

## Scope

- Configure Quarkus application packaging for backend
- Configure React production build for frontend
- Set up artifact generation and organization
- Configure build optimization and validation
- Add build verification and quality checks

## Out of Scope

- Docker image building
- Cloud deployment configuration
- Artifact repository publishing
- Advanced build caching strategies

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0030-standardize_product_workspace_build_workflow_with_gradle-create_backend_gradle_build_configuration.md
- 0031-standardize_product_workspace_build_workflow_with_gradle-create_frontend_gradle_build_configuration.md

## Implementation Details

Backend build configuration (extend backend/build.gradle):
```gradle
// Quarkus build configuration
quarkus {
    buildNative {
        enabled = false // JVM mode by default
    }
    
    buildForkOptions {
        systemProperties = [
            'quarkus.package.type': 'uber-jar'
        ]
    }
}

// Custom JAR configuration
jar {
    enabled = true
    archiveClassifier = 'classes'
}

// Fat JAR for easy deployment
task fatJar(type: Jar) {
    group = 'build'
    description = 'Creates a fat JAR with all dependencies'
    archiveClassifier = 'fat'
    
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    with jar
    
    manifest {
        attributes(
            'Main-Class': 'io.quarkus.runner.GeneratedMain',
            'Implementation-Title': project.name,
            'Implementation-Version': project.version
        )
    }
}

// Build verification
task verifyBuild {
    group = 'verification'
    description = 'Verifies the build output'
    dependsOn 'build'
    
    doLast {
        def jarFile = file("${buildDir}/quarkus-app/quarkus-run.jar")
        if (!jarFile.exists()) {
            throw new GradleException("Expected JAR file not found: ${jarFile}")
        }
        println "Build verification passed. JAR size: ${jarFile.length()} bytes"
    }
}

// Application info task
task appInfo {
    group = 'help'
    description = 'Displays application build information'
    
    doLast {
        println "Application: ${project.name}"
        println "Version: ${project.version}"
        println "Java Version: ${java.sourceCompatibility}"
        println "Build Dir: ${buildDir}"
    }
}
```

Frontend build configuration (extend frontend/build.gradle):
```gradle
// Production build with optimization
task buildProd(type: NpmTask) {
    group = 'build'
    description = 'Builds optimized production bundle'
    dependsOn 'npmInstall'
    npmCommand = ['run', 'build']
    
    inputs.dir('src')
    inputs.dir('public')
    inputs.file('package.json')
    outputs.dir('build')
    
    doFirst {
        println "Building React application for production..."
    }
    
    doLast {
        def buildDir = file('build')
        if (!buildDir.exists()) {
            throw new GradleException("Frontend build failed - build directory not created")
        }
        
        def staticDir = file('build/static')
        if (staticDir.exists()) {
            def jsFiles = fileTree(staticDir).include('**/*.js').files.size()
            def cssFiles = fileTree(staticDir).include('**/*.css').files.size()
            println "Frontend build completed: ${jsFiles} JS files, ${cssFiles} CSS files"
        }
    }
}

// Build size analysis
task analyzeBuild(type: NpmTask) {
    group = 'verification'
    description = 'Analyzes the production build size'
    dependsOn 'buildProd'
    npmCommand = ['run', 'analyze']
}

// Build verification
task verifyBuild {
    group = 'verification'
    description = 'Verifies the frontend build output'
    dependsOn 'buildProd'
    
    doLast {
        def indexFile = file('build/index.html')
        if (!indexFile.exists()) {
            throw new GradleException("Expected index.html not found in build output")
        }
        
        def buildSize = fileTree('build').files.sum { it.length() }
        println "Frontend build verification passed. Total size: ${buildSize} bytes"
    }
}
```

Root build orchestration (extend root build.gradle):
```gradle
// Workspace build task
task buildWorkspace {
    group = 'build'
    description = 'Builds the entire workspace with verification'
    dependsOn ':backend:verifyBuild', ':frontend:verifyBuild'
    
    doLast {
        println "=== Workspace Build Summary ==="
        println "Backend JAR: backend/build/quarkus-app/quarkus-run.jar"
        println "Frontend build: frontend/build/"
        println "Build completed successfully!"
    }
}

// Release packaging
task packageRelease(type: Zip) {
    group = 'release'
    description = 'Packages the complete application for release'
    dependsOn 'buildWorkspace'
    
    archiveFileName = "${project.name}-${project.version}.zip"
    destinationDirectory = file("${buildDir}/distributions")
    
    from('backend/build/quarkus-app') {
        into 'backend'
    }
    
    from('frontend/build') {
        into 'frontend'
    }
    
    from('.') {
        include 'README.md', 'LICENSE'
        include 'docker-compose.yml'
        include 'gradlew*'
        include 'gradle/**'
    }
    
    doLast {
        println "Release package created: ${archiveFile.get().asFile}"
    }
}

// Quick build verification
task quickBuild {
    group = 'build'
    description = 'Quick build without full verification'
    dependsOn ':backend:build', ':frontend:buildProd'
}
```

Build optimization configuration:
```gradle
// Gradle build optimization
gradle.projectsEvaluated {
    tasks.withType(JavaCompile) {
        options.compilerArgs << '-Xlint:unchecked'
        options.deprecation = true
    }
}

// Parallel builds
org.gradle.parallel=true
org.gradle.caching=true
```

## Files / Modules Impacted

- backend/build.gradle (extend build configuration)
- frontend/build.gradle (extend build configuration)
- build.gradle (root - add workspace build tasks)
- gradle.properties (add build optimization)
- frontend/package.json (add analyze script if needed)

## Acceptance Criteria

Given the build configuration is complete
When ./gradlew buildWorkspace is executed
Then both backend and frontend should build successfully with verification

Given the packaging is configured
When ./gradlew packageRelease is executed
Then a complete release package should be created

Given build verification is implemented
When builds complete
Then output artifacts should be validated automatically

Given build optimization is configured
When builds are executed
Then they should complete efficiently with proper caching

## Testing Requirements

- Test backend JAR creation and validation
- Test frontend production build
- Test workspace build orchestration
- Test release packaging
- Test build verification and error handling

## Dependencies / Preconditions

- Backend and frontend build configurations must exist
- Source code must be in buildable state
- All dependencies must be resolvable
- Build tools must be properly configured