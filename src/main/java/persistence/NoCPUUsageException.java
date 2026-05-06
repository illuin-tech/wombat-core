package persistence;

public class NoCPUUsageException extends Exception {
    public NoCPUUsageException(String message) {
        super(message);
    }
}
