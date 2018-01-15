package de.hu_berlin.slice.plugin.jobs;

/**
 * Exception for a slicing failure.
 * @author IShowerNaked
 */
class TaskException extends Exception {

    private static final long serialVersionUID = 1L;

    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }
}