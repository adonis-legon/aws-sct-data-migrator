package app.alegon.aws.sct.migrator.persistence.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import app.alegon.aws.sct.migrator.config.MigrationLoaderConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.persistence.DataLoader;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;
import app.alegon.aws.sct.migrator.persistence.sql.exception.SqlConnectionStringException;

public abstract class SqlDataLoader extends DataLoader {

    protected Connection targetConnection;

    protected boolean isInitialized;

    public SqlDataLoader(MigrationLoaderConfig migrationLoaderConfig) {
        super(migrationLoaderConfig);
    }

    @Override
    public void initialize(MigrationDataSource migrationDataSource) throws DataExtractorException {
        try {
            if (targetConnection == null || targetConnection.isClosed()) {
                targetConnection = DriverManager.getConnection(getConnectionString(migrationDataSource),
                        migrationDataSource.userName(), migrationDataSource.password());
                isInitialized = true;
            }
        } catch (SQLException | SqlConnectionStringException e) {
            throw new DataExtractorException("Error connecting to target datasource. Message: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void terminate() throws DataExtractorException {
        try {
            if (targetConnection != null && !targetConnection.isClosed()) {
                targetConnection.close();
            }
        } catch (SQLException e) {
            throw new DataExtractorException(
                    "Error closing connection to target datasource. Message: " + e.getMessage(), e);
        }
    }

    protected abstract String getConnectionString(MigrationDataSource migrationDataSource)
            throws SqlConnectionStringException;
}
