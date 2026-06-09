package io.github.ahmedbulbul.autoport.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortScannerTest {

    @Test
    void findsAvailablePortInRange() {
        int port = PortScanner.findAvailablePort(18080, 19000);
        assertThat(port).isBetween(18080, 19000);
        assertThat(PortScanner.isPortAvailable(port)).isTrue();
    }

    @Test
    void skipsOccupiedPortAndFindsNext() throws IOException {
        int base = PortScanner.findAvailablePort(18080, 19000);
        try (ServerSocket ignored = new ServerSocket(base)) {
            int next = PortScanner.findAvailablePort(base, 19000);
            assertThat(next).isGreaterThan(base);
        }
    }

    @Test
    void returnsSamePortWhenStartPortIsAvailable() {
        int port = PortScanner.findAvailablePort(18080, 19000);
        // Port is available, so findAvailablePort starting there must return it
        assertThat(PortScanner.findAvailablePort(port, 19000)).isEqualTo(port);
    }

    @Test
    void throwsPortScanExceptionWhenRangeExhausted() throws IOException {
        int port = PortScanner.findAvailablePort(18080, 19000);
        try (ServerSocket ignored = new ServerSocket(port)) {
            // Range of exactly one port, and it is occupied
            assertThatThrownBy(() -> PortScanner.findAvailablePort(port, port))
                .isInstanceOf(PortScanException.class)
                .hasMessageContaining("No available port");
        }
    }

    @Test
    void throwsIllegalArgumentForInvalidStartPort() {
        assertThatThrownBy(() -> PortScanner.findAvailablePort(0, 9000))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PortScanner.findAvailablePort(99999, 99999))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsIllegalArgumentWhenMaxBelowStart() {
        assertThatThrownBy(() -> PortScanner.findAvailablePort(9000, 8000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isPortAvailableReturnsFalseForOccupiedPort() throws IOException {
        int port = PortScanner.findAvailablePort(18080, 19000);
        try (ServerSocket ignored = new ServerSocket(port)) {
            assertThat(PortScanner.isPortAvailable(port)).isFalse();
        }
    }
}
