package app.alegon.aws.sct.migrator.persistence.exception;

public class DataExtractorException extends Exception {
    public DataExtractorException(String message, Throwable reason) {
        super("Error exporting data. Message: " + message, reason);
    }
}
