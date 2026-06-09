package io.github.ahmedbulbul.autoport;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutoPortEnvironmentPostProcessorTest {

    // DeferredLogFactory's abstract method is getLog(Supplier<Log>);
    // Supplier::get satisfies it: (Supplier<Log> s) -> s.get()
    private final AutoPortEnvironmentPostProcessor processor =
        new AutoPortEnvironmentPostProcessor(Supplier::get);

    // -----------------------------------------------------------------------
    // Happy path — port is occupied
    // -----------------------------------------------------------------------

    @Test
    void assignsNextPortWhenStartPortIsOccupied() throws IOException {
        int base = 18090;
        try (ServerSocket ignored = new ServerSocket(base)) {
            StandardEnvironment env = envWith("auto.port.start", base, "auto.port.max", 19000);

            processor.postProcessEnvironment(env, null);

            int resolved = env.getProperty("server.port", Integer.class);
            assertThat(resolved).isGreaterThan(base).isLessThanOrEqualTo(19000);
        }
    }

    @Test
    void assignsNextPortWhenExplicitServerPortIsOccupied() throws IOException {
        int base = 18091;
        try (ServerSocket ignored = new ServerSocket(base)) {
            StandardEnvironment env = envWith("server.port", base, "auto.port.max", 19000);

            processor.postProcessEnvironment(env, null);

            int resolved = env.getProperty("server.port", Integer.class);
            assertThat(resolved).isGreaterThan(base).isLessThanOrEqualTo(19000);
        }
    }

    // -----------------------------------------------------------------------
    // Happy path — port is free
    // -----------------------------------------------------------------------

    @Test
    void keepsConfiguredPortWhenAvailable() {
        // Find a free port first, then configure it explicitly
        int free = findFreePort(18092, 19000);
        StandardEnvironment env = envWith("server.port", free);

        processor.postProcessEnvironment(env, null);

        // Must not be overridden when it is already free
        assertThat(env.getProperty("server.port", Integer.class)).isEqualTo(free);
        // Our property source should not have been added
        assertThat(env.getPropertySources().contains(AutoPortEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
            .isFalse();
    }

    @Test
    void setsStartPortExplicitlyWhenNotConfigured() {
        int start = 18093;
        StandardEnvironment env = envWith("auto.port.start", start, "auto.port.max", 19000);

        processor.postProcessEnvironment(env, null);

        int resolved = env.getProperty("server.port", Integer.class);
        assertThat(resolved).isGreaterThanOrEqualTo(start).isLessThanOrEqualTo(19000);
    }

    // -----------------------------------------------------------------------
    // Disabled / opt-out scenarios
    // -----------------------------------------------------------------------

    @Test
    void doesNothingWhenDisabled() throws IOException {
        int base = 18094;
        try (ServerSocket ignored = new ServerSocket(base)) {
            StandardEnvironment env = envWith(
                "auto.port.enabled", false,
                "auto.port.start", base,
                "auto.port.max", 19000
            );

            processor.postProcessEnvironment(env, null);

            // Property source must not have been injected
            assertThat(env.getPropertySources().contains(AutoPortEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
        }
    }

    @Test
    void doesNothingForRandomPortMode() {
        StandardEnvironment env = envWith("server.port", "0");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getPropertySources().contains(AutoPortEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
            .isFalse();
    }

    // -----------------------------------------------------------------------
    // Error path — range exhausted
    // -----------------------------------------------------------------------

    @Test
    void throwsIllegalStateWhenRangeExhausted() throws IOException {
        // start == max and that single port is occupied
        int port = findFreePort(18095, 19000);
        try (ServerSocket ignored = new ServerSocket(port)) {
            StandardEnvironment env = envWith(
                "auto.port.start", port,
                "auto.port.max",   port   // only one port in range, and it is occupied
            );

            assertThatThrownBy(() -> processor.postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no available port found");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static StandardEnvironment envWith(Object... keyValues) {
        StandardEnvironment env = new StandardEnvironment();
        Map<String, Object> props = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            props.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        env.getPropertySources().addFirst(new MapPropertySource("test", props));
        return env;
    }

    private static int findFreePort(int start, int max) {
        for (int p = start; p <= max; p++) {
            try (ServerSocket s = new ServerSocket(p)) {
                return p;
            } catch (IOException ignored) {
            }
        }
        throw new IllegalStateException("No free port found between " + start + " and " + max);
    }
}
