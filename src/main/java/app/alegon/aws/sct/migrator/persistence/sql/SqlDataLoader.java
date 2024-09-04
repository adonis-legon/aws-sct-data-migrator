package app.alegon.aws.sct.migrator.persistence.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import app.alegon.aws.sct.migrator.config.MigrationLoaderConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.DataLoader;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;
import app.alegon.aws.sct.migrator.persistence.sql.exception.SqlConnectionStringException;

public abstract class SqlDataLoader extends DataLoader {

    private final String TEMPORAL_TABLE_NAME = "temp_table_%s";

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
    public void loadTable(String tableData, MigrationTable targetTable, MigrationDataSource migrationDataSource)
            throws DataLoaderException {
        try (Statement stmt = targetConnection.createStatement()) {
            String temporalTableName = getTemporalTableName(targetTable);

            // create temporal table before data insertion
            String createTemporalTableSQL = String.format(getCreateTemporalTableSql(targetTable), temporalTableName,
                    targetTable.parentSchema().name(), targetTable.name());
            stmt.execute(createTemporalTableSQL);

            // copy data into the temporal table
            copyDataIntoTemporalTable(tableData, targetTable, stmt);

            // try to insert data into target table, and is there is any PK violation due to
            // duplicates, just skip it
            insertFromTemporalTableIntoTargetTable(targetTable, stmt);

            // drop temporal table
            String dropTemporalTableSQL = getDropTemporalTableSql(targetTable);
            stmt.execute(dropTemporalTableSQL);
            stmt.close();
        } catch (SQLException | DataLoaderException e) {
            throw new DataLoaderException(e.getMessage(), e);
        }
    }

    protected String getTemporalTableName(MigrationTable targetTable) {
        return String.format(TEMPORAL_TABLE_NAME, targetTable.name());
    }

    protected abstract String getCreateTemporalTableSql(MigrationTable targetTable);

    protected abstract void copyDataIntoTemporalTable(String tableData, MigrationTable targetTable, Statement stmt)
            throws DataLoaderException;

    protected abstract void insertFromTemporalTableIntoTargetTable(MigrationTable targetTable, Statement stmt)
            throws DataLoaderException;

    protected abstract String getDropTemporalTableSql(MigrationTable targetTable);

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
