package io.github.ahmedbulbul.autoport.core;

public class PortScanException extends RuntimeException {

    public PortScanException(String message) {
        super(message);
    }

    public PortScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
