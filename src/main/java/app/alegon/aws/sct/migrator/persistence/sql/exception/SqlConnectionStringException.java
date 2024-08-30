package app.alegon.aws.sct.migrator.persistence.sql.exception;

public class SqlConnectionStringException extends Exception {
    public SqlConnectionStringException(String message, Throwable reason) {
        super(message, reason);
    }
}
