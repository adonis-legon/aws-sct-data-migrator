package app.alegon.aws.sct.migrator.persistence;

import app.alegon.aws.sct.migrator.config.MigrationLoaderConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;

public abstract class DataLoader {

    protected MigrationLoaderConfig migrationLoaderConfig;

    public DataLoader(MigrationLoaderConfig migrationLoaderConfig) {
        this.migrationLoaderConfig = migrationLoaderConfig;
    }

    public abstract void initialize(MigrationDataSource migrationDataSource) throws DataExtractorException;

    public abstract boolean isInitialized();

    public abstract void loadTable(String tableData, MigrationTable targetTable,
            MigrationDataSource migrationDataSource) throws DataLoaderException;

    public abstract void terminate() throws DataExtractorException;
}
