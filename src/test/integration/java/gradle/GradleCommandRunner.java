package gradle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class GradleCommandRunner {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(20);
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final String GRADLEW = System.getProperty("os.name").toLowerCase().contains("windows")
        ? "gradlew.bat"
        : "./gradlew";

    ProcessResult run(Duration timeout, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(GRADLEW);
        command.add("--no-parallel");
        command.add("--max-workers=1");
        command.addAll(List.of(arguments));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(PROJECT_ROOT.toFile());
        processBuilder.redirectErrorStream(true);

        Map<String, String> environment = processBuilder.environment();
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            environment.put("JAVA_HOME", javaHome);
        }

        Process process = processBuilder.start();
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        Thread outputReader = new Thread(() -> {
            try {
                process.getInputStream().transferTo(outputBuffer);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
        outputReader.start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            throw new IllegalStateException("Timed out waiting for command: " + String.join(" ", command));
        }

        outputReader.join(TimeUnit.SECONDS.toMillis(5));
        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), output);
    }

    ProcessResult run(String... arguments) throws IOException, InterruptedException {
        return run(DEFAULT_TIMEOUT, arguments);
    }

    record ProcessResult(int exitCode, String output) {
    }
}
