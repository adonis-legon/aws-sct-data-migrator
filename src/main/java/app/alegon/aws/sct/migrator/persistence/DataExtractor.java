package app.alegon.aws.sct.migrator.persistence;

import app.alegon.aws.sct.migrator.config.MigrationConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;

public abstract class DataExtractor {

    protected MigrationConfig migrationConfig;

    public DataExtractor(MigrationConfig migrationConfig) {
        this.migrationConfig = migrationConfig;
    }

    public abstract String extractTable(MigrationTable table, MigrationDataSource migrationDataSource)
            throws DataExtractorException;
}
