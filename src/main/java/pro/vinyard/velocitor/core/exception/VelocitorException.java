package pro.vinyard.velocitor.core.exception;

public class VelocitorException extends Exception {
    public VelocitorException(String message) {
        super(message);
    }

    public VelocitorException(String message, Throwable cause) {
        super(message, cause);
    }
}
