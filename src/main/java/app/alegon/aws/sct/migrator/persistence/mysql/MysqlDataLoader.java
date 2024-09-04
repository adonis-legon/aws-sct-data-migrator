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
    private final String CREATE_TEMPORAL_TABLE_SQL = "CREATE TEMPORARY TABLE %s LIKE %s";
    private final String TABLE_COPY_COMMAND = "LOAD DATA LOCAL INFILE '%s' INTO TABLE %s FIELDS TERMINATED BY '%s' ENCLOSED BY '\"' LINES TERMINATED BY '\\n' IGNORE 1 ROWS;";
    private final String INSERT_TEMPORAL_TABLE_SQL = "INSERT IGNORE INTO %s SELECT * FROM %s";
    private final String DROP_TEMPORAL_TABLE_SQL = "DROP TEMPORARY TABLE IF EXISTS %s";

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
    protected String getCreateTemporalTableSql(MigrationTable targetTable) {
        return String.format(CREATE_TEMPORAL_TABLE_SQL, getTemporalTableName(targetTable), targetTable.name());
    }

    @Override
    protected void copyDataIntoTemporalTable(String tableData, MigrationTable targetTable, Statement stmt)
            throws DataLoaderException {
        try {
            String tableCopyCommand = String.format(TABLE_COPY_COMMAND, tableData, getTemporalTableName(targetTable),
                    migrationLoaderConfig.getCsvSeparator());
            stmt.execute(tableCopyCommand);
        } catch (SQLException e) {
            throw new DataLoaderException("Error copying data into temporal table: " + targetTable.name(), e);
        }
    }

    @Override
    protected void insertFromTemporalTableIntoTargetTable(MigrationTable targetTable, Statement stmt)
            throws DataLoaderException {
        try {
            String insertTemporalTableSQL = String.format(INSERT_TEMPORAL_TABLE_SQL, targetTable.name(),
                    getTemporalTableName(targetTable));
            stmt.execute(insertTemporalTableSQL);
        } catch (SQLException e) {
            throw new DataLoaderException("Error inserting data into target table: " + targetTable.name(), e);
        }
    }

    @Override
    protected String getDropTemporalTableSql(MigrationTable targetTable) {
        return String.format(DROP_TEMPORAL_TABLE_SQL, getTemporalTableName(targetTable));
    }

}
