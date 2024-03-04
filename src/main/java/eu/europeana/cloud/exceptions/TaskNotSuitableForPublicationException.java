package eu.europeana.cloud.exceptions;

public class TaskNotSuitableForPublicationException extends Exception {
    public TaskNotSuitableForPublicationException(String message) {
        super(message);
    }

    public TaskNotSuitableForPublicationException() {
        super("Task is not suitable for publication!");
    }

    public TaskNotSuitableForPublicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskNotSuitableForPublicationException(Throwable cause) {
        super(cause);
    }
}
