package app.alegon.aws.sct.migrator.persistence.mysql;

import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationLoaderConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;
import app.alegon.aws.sct.migrator.persistence.sql.SqlDataLoader;
import app.alegon.aws.sct.migrator.persistence.sql.exception.SqlConnectionStringException;

@Component("MYSQL")
public class MysqlDataLoader extends SqlDataLoader {

    private final String MYSQL_CONNECTION_STRING = "jdbc:mysql://%s:%s?allowLoadLocalInfile=true";
    private final String TABLE_COPY_COMMAND = "LOAD DATA LOCAL INFILE '%s' INTO TABLE %s FIELDS TERMINATED BY '%s' ENCLOSED BY '\"' LINES TERMINATED BY '\\n' IGNORE 1 ROWS;";

    public MysqlDataLoader(MigrationLoaderConfig migrationLoaderConfig) {
        super(migrationLoaderConfig);
    }

    @Override
    public void initialize(MigrationDataSource migrationDataSource) throws DataExtractorException {
        super.initialize(migrationDataSource);
        try {
            targetConnection.setCatalog("`" + migrationDataSource.database() + "`");
        } catch (SQLException e) {
            throw new DataExtractorException("Error setting MySQL Catalog. Message: " + e.getMessage(), e);
        }
    }

    @Override
    protected String getConnectionString(MigrationDataSource migrationDataSource) throws SqlConnectionStringException {
        return String.format(MYSQL_CONNECTION_STRING, migrationDataSource.host(), migrationDataSource.port());
    }

    @Override
    public void loadTable(String tableData, MigrationTable targetTable, MigrationDataSource migrationDataSource)
            throws DataLoaderException {
        try {
            String tableCopyCommand = String.format(TABLE_COPY_COMMAND, tableData, targetTable.name(),
                    migrationLoaderConfig.getCsvSeparator());
            try (Statement stmt = targetConnection.createStatement()) {
                stmt.execute(tableCopyCommand);
            }
        } catch (Exception e) {
            throw new DataLoaderException(e.getMessage(), e);
        }
    }

}
