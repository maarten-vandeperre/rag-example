package gradle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentModeTest {
    private final GradleCommandRunner gradle = new GradleCommandRunner();

    @Test
    void shouldWireDevelopmentModeTasksForBackendAndFrontend() throws Exception {
        GradleCommandRunner.ProcessResult result = gradle.run("dev", "--dry-run");

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(":backend:dev");
        assertThat(result.output()).contains(":frontend:dev");
        assertThat(result.output()).contains(":dev");
    }
}
