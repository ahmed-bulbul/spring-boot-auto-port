package io.github.ahmedbulbul.autoport.core;

import java.io.IOException;
import java.net.ServerSocket;

public final class PortScanner {

    private PortScanner() {}

    /**
     * Scans sequentially from {@code startPort} to {@code maxPort} (inclusive)
     * and returns the first available port.
     *
     * @throws IllegalArgumentException if the port range is invalid
     * @throws PortScanException        if no available port is found in the range
     */
    public static int findAvailablePort(int startPort, int maxPort) {
        if (startPort < 1 || startPort > 65535) {
            throw new IllegalArgumentException(
                "startPort must be between 1 and 65535, got: " + startPort);
        }
        if (maxPort < startPort || maxPort > 65535) {
            throw new IllegalArgumentException(
                "maxPort must be between startPort (" + startPort + ") and 65535, got: " + maxPort);
        }
        for (int port = startPort; port <= maxPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new PortScanException(
            "No available port found in range [" + startPort + ", " + maxPort + "]. "
            + "Increase auto.port.max or free up ports.");
    }

    /**
     * Returns {@code true} if the given port can be bound on the local machine.
     */
    public static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
