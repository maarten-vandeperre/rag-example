package gradle;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

class BuildProcessTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void shouldBuildBackendSuccessfully() throws Exception {
        Path libsDir = PROJECT_ROOT.resolve("backend/build/libs");
        Path classesJar = locateArtifact(libsDir, "-classes.jar");
        Path fatJar = locateArtifact(libsDir, "-fat.jar");

        assertThat(Files.size(classesJar)).isGreaterThan(1024);
        assertThat(Files.size(fatJar)).isGreaterThan(Files.size(classesJar));
    }

    @Test
    void shouldBuildFrontendSuccessfully() {
        Path buildDir = PROJECT_ROOT.resolve("frontend/build");

        assertThat(buildDir.resolve("index.html")).exists();
        assertThat(buildDir.resolve("static")).isDirectory();
    }

    @Test
    void shouldPackageWorkspaceReleaseArchive() throws Exception {
        Path archive = locateArtifact(PROJECT_ROOT.resolve("build/distributions"), ".zip");

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            assertThat(zipFile.stream().anyMatch(entry -> entry.getName().matches("backend/libs/.+-classes\\.jar"))).isTrue();
            assertThat(zipFile.stream().anyMatch(entry -> entry.getName().matches("backend/libs/.+-fat\\.jar"))).isTrue();
            assertThat(zipFile.getEntry("frontend/index.html")).isNotNull();
            assertThat(zipFile.stream().anyMatch(entry -> entry.getName().startsWith("frontend/static/js/") && entry.getName().endsWith(".js"))).isTrue();
        }
    }

    private Path locateArtifact(Path directory, String suffix) throws Exception {
        try (var files = Files.list(directory)) {
            return files
                .filter(path -> path.getFileName().toString().endsWith(suffix))
                .max(Comparator.comparing(path -> path.getFileName().toString()))
                .orElseThrow();
        }
    }
}
