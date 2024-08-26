package app.alegon.aws.sct.migrator.persistence.exception;

import app.alegon.aws.sct.migrator.model.MigrationTableColumn;

public class DataSerializeException extends Exception {
    public DataSerializeException(MigrationTableColumn migrationTableColumn, Throwable reason) {
        super("Error serializing column " + migrationTableColumn.name() + ". Message: " + reason.getMessage(), reason);
    }
}
