package app.alegon.aws.sct.migrator.persistence;

import app.alegon.aws.sct.migrator.config.MigrationExtractorConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;

public abstract class DataExtractor {

    protected MigrationExtractorConfig migrationExtractorConfig;

    public DataExtractor(MigrationExtractorConfig migrationExtractorConfig) {
        this.migrationExtractorConfig = migrationExtractorConfig;
    }

    public abstract void initialize(MigrationDataSource migrationDataSource) throws DataExtractorException;

    public abstract boolean isInitialized();

    public abstract String extractTable(MigrationTable table, MigrationDataSource migrationDataSource)
            throws DataExtractorException;

    public abstract void terminate() throws DataExtractorException;
}
