package app.alegon.aws.sct.migrator.persistence.exception;

public class DataTransporterReceiveException extends Exception {
    public DataTransporterReceiveException(String message, Throwable reason) {
        super(message, reason);
    }
}
