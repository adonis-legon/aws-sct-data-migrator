package app.alegon.aws.sct.migrator.provider.exception;

public class MigrationSchemaInvalidFormatException extends Exception {
    public MigrationSchemaInvalidFormatException(String message, Throwable reason) {
        super(message, reason);
    }
}
