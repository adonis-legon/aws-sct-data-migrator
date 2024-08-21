package app.alegon.aws.sct.migrator.persistence.exception;

public class DataLoaderException extends Exception {

    public DataLoaderException(String message, Throwable reason) {
        super("Error importing data. Message: " + message, reason);
    }
}
