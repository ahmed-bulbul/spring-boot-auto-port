package io.github.ahmedbulbul.autoport;

import io.github.ahmedbulbul.autoport.core.PortScanException;
import io.github.ahmedbulbul.autoport.core.PortScanner;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Fires during environment preparation (before the server starts) and rewrites
 * {@code server.port} to the next available port when the base port is occupied.
 *
 * <p>Configuration properties (all optional):
 * <ul>
 *   <li>{@code auto.port.enabled}  — set {@code false} to disable (default: {@code true})</li>
 *   <li>{@code auto.port.start}    — base port when {@code server.port} is unset (default: 8080)</li>
 *   <li>{@code auto.port.max}      — upper bound of the scan range (default: 9000)</li>
 * </ul>
 */
public class AutoPortEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "autoPortPropertySource";

    private static final String KEY_ENABLED    = "auto.port.enabled";
    private static final String KEY_START      = "auto.port.start";
    private static final String KEY_MAX        = "auto.port.max";
    private static final String KEY_SERVER_PORT = "server.port";

    private static final int DEFAULT_START = 8080;
    private static final int DEFAULT_MAX   = 9000;

    private final Log log;

    // Spring Boot injects DeferredLogFactory so log output is replayed after
    // the logging system initialises (avoids lost early-startup messages).
    public AutoPortEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        boolean enabled = environment.getProperty(KEY_ENABLED, Boolean.class, true);
        if (!enabled) {
            log.debug("Auto port detection is disabled (auto.port.enabled=false)");
            return;
        }

        String rawServerPort = environment.getProperty(KEY_SERVER_PORT);

        // Respect server.port=0 — Spring Boot's own random-port mode
        if ("0".equals(rawServerPort)) {
            log.debug("server.port=0 detected; skipping auto port resolution");
            return;
        }

        int startPort = environment.getProperty(KEY_START, Integer.class, DEFAULT_START);
        int maxPort   = environment.getProperty(KEY_MAX,   Integer.class, DEFAULT_MAX);

        // Resolve the port we should start from
        int basePort = rawServerPort != null ? parsePort(rawServerPort, startPort) : startPort;

        if (PortScanner.isPortAvailable(basePort)) {
            if (rawServerPort != null) {
                // User explicitly configured this port and it is free — do not interfere
                log.debug("Port " + basePort + " is available; keeping configured server.port");
                return;
            }
            // No server.port set: write our start port so it appears in the environment
            // (handles the case where auto.port.start differs from Spring's 8080 default)
            log.info("Auto port: port " + basePort + " is available, assigning as server.port");
            setPort(environment, basePort);
            return;
        }

        log.info("Auto port: port " + basePort + " is already in use, scanning for next available port...");
        try {
            int resolved = PortScanner.findAvailablePort(basePort + 1, maxPort);
            log.info("Auto port: assigned port " + resolved
                + " (port " + basePort + " was occupied)");
            setPort(environment, resolved);
        } catch (PortScanException | IllegalArgumentException ex) {
            throw new IllegalStateException(
                "spring-boot-auto-port: no available port found in range ["
                + (basePort + 1) + ", " + maxPort + "]. "
                + "Increase auto.port.max or free some ports.", ex);
        }
    }

    // Runs last so all user-defined EnvironmentPostProcessors run first,
    // giving us the final picture of server.port before we act.
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void setPort(ConfigurableEnvironment environment, int port) {
        Map<String, Object> props = Map.of(KEY_SERVER_PORT, port);
        environment.getPropertySources()
            .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    private int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid server.port value '" + value + "'; falling back to " + fallback);
            return fallback;
        }
    }
}
