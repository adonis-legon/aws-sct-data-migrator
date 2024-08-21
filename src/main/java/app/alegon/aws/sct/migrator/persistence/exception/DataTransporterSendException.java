package app.alegon.aws.sct.migrator.persistence.exception;

public class DataTransporterSendException extends Exception {
    public DataTransporterSendException(String message, Throwable reason) {
        super(message, reason);
    }
}
