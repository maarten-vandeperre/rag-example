package gradle;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowIntegrationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private final GradleCommandRunner gradle = new GradleCommandRunner();

    @Test
    @Order(1)
    void shouldValidateEnvironmentSetup() throws Exception {
        GradleCommandRunner.ProcessResult result = gradle.run("healthCheck");

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains("Health Check Passed");
        assertThat(PROJECT_ROOT.resolve("gradle/wrapper/gradle-wrapper.properties")).exists();
        assertThat(PROJECT_ROOT.resolve("setup.sh")).exists();
    }

    @Test
    @Order(2)
    void shouldInstallFrontendDependencies() {
        assertThat(PROJECT_ROOT.resolve("frontend/node_modules")).exists();
    }

    @Test
    @Order(3)
    void shouldRunAllTests() {
        assertThat(PROJECT_ROOT.resolve("build/reports/allTests/backend/index.html")).exists();
        assertThat(PROJECT_ROOT.resolve("build/reports/allTests/frontend/index.html")).exists();
    }

    @Test
    @Order(4)
    void shouldBuildWorkspace() throws Exception {
        Path libsDir = PROJECT_ROOT.resolve("backend/build/libs");

        assertThat(hasArtifact(libsDir, "-classes.jar")).isTrue();
        assertThat(hasArtifact(libsDir, "-fat.jar")).isTrue();
        assertThat(PROJECT_ROOT.resolve("frontend/build/index.html")).exists();
    }

    @Test
    @Order(5)
    void shouldPrepareRelease() {
        assertThat(PROJECT_ROOT.resolve("build/distributions")).isDirectory();
        assertThat(PROJECT_ROOT.resolve("README.md")).exists();
    }

    @Test
    @Order(6)
    void shouldPackageRelease() throws Exception {
        Path releasePackage;
        try (var packages = Files.list(PROJECT_ROOT.resolve("build/distributions"))) {
            releasePackage = packages
                .filter(path -> path.getFileName().toString().endsWith(".zip"))
                .max(Comparator.comparing(path -> path.getFileName().toString()))
                .orElseThrow();
        }
        
        assertThat(Files.size(releasePackage)).isPositive();
    }

    private boolean hasArtifact(Path directory, String suffix) throws Exception {
        try (var files = Files.list(directory)) {
            return files.anyMatch(path -> path.getFileName().toString().endsWith(suffix));
        }
    }
}
