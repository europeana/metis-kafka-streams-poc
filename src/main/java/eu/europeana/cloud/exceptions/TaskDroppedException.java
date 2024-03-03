package eu.europeana.cloud.exceptions;

public class TaskDroppedException extends Exception {
    public TaskDroppedException(String message) {
        super(message);
    }

    public TaskDroppedException() {
        super("Task was dropped by user");
    }

    public TaskDroppedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskDroppedException(Throwable cause) {
        super(cause);
    }
}
