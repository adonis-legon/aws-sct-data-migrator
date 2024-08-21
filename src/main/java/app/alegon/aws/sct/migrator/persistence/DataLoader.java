package app.alegon.aws.sct.migrator.persistence;

import app.alegon.aws.sct.migrator.config.MigrationConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;

public abstract class DataLoader {
    protected MigrationConfig migrationConfig;

    public DataLoader(MigrationConfig migrationConfig) {
        this.migrationConfig = migrationConfig;
    }

    public abstract void loadTable(String tableData, MigrationTable targetTable,
            MigrationDataSource migrationDataSource) throws DataLoaderException;
}
