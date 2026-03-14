package gradle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {
    private final GradleCommandRunner gradle = new GradleCommandRunner();

    @Test
    void shouldProvideHelpfulErrorMessageOnFailure() throws Exception {
        GradleCommandRunner.ProcessResult result = gradle.run("definitelyMissingTask");

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("BUILD FAILED");
        assertThat(result.output()).contains("For escalation to the lead architect, include:");
        assertThat(result.output()).contains("Gradle version:");
        assertThat(result.output()).contains("Java version:");
    }

    @Test
    void shouldCollectDiagnosticInformation() throws Exception {
        GradleCommandRunner.ProcessResult result = gradle.run("diagnostics");

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains("Diagnostic Information");
        assertThat(result.output()).contains("Gradle Version:");
        assertThat(result.output()).contains("Java Version:");
        assertThat(result.output()).contains("Available Tasks:");
    }
}
