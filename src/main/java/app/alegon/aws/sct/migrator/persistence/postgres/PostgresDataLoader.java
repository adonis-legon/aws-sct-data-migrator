package app.alegon.aws.sct.migrator.persistence.postgres;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationLoaderConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;
import app.alegon.aws.sct.migrator.persistence.sql.SqlDataLoader;
import app.alegon.aws.sct.migrator.persistence.sql.exception.SqlConnectionStringException;

@Component("POSTGRESQL")
public class PostgresDataLoader extends SqlDataLoader {

    private final String POSTGRES_CONNECTION_STRING = "jdbc:postgresql://%s:%s/%s";
    private final String CREATE_TEMPORAL_TABLE_SQL = "CREATE TEMPORARY TABLE %s AS TABLE \"%s\".\"%s\" WITH NO DATA";
    private final String TABLE_COPY_COMMAND = "COPY %s (%s) FROM STDIN WITH (FORMAT CSV, HEADER true, DELIMITER '%s', NULL 'null')";
    private final String INSERT_TEMPORAL_TABLE_SQL = "INSERT INTO \"%s\".\"%s\" SELECT * FROM %s ON CONFLICT (%s) DO NOTHING";
    private final String DROP_TEMPORAL_TABLE_SQL = "DROP TABLE %s";

    public PostgresDataLoader(MigrationLoaderConfig migrationLoaderConfig) {
        super(migrationLoaderConfig);
    }

    @Override
    protected String getConnectionString(MigrationDataSource migrationDataSource) throws SqlConnectionStringException {
        return String.format(POSTGRES_CONNECTION_STRING, migrationDataSource.host(), migrationDataSource.port(),
                migrationDataSource.database());
    }

    @Override
    protected String getCreateTemporalTableSql(MigrationTable targetTable) {
        return String.format(CREATE_TEMPORAL_TABLE_SQL, getTemporalTableName(targetTable),
                targetTable.parentSchema().name(), targetTable.name());
    }

    @Override
    protected void copyDataIntoTemporalTable(String tableData, MigrationTable targetTable, Statement stmt)
            throws DataLoaderException {
        try {
            CopyManager copyManager = new CopyManager((BaseConnection) targetConnection);
            StringReader reader = new StringReader(Files.readString(Paths.get(tableData)));

            String columnList = targetTable.columns().stream().map(col -> "\"" + col.name() + "\"")
                    .collect(Collectors.joining(","));

            String copyQuery = String.format(TABLE_COPY_COMMAND, getTemporalTableName(targetTable), columnList,
                    migrationLoaderConfig.getCsvSeparator());

            copyManager.copyIn(copyQuery, reader);
        } catch (SQLException | IOException e) {
            throw new DataLoaderException("Error copying data into temporal table from the file: " + tableData, e);
        }
    }

    @Override
    protected void insertFromTemporalTableIntoTargetTable(MigrationTable targetTable, Statement stmt)
            throws DataLoaderException {
        try {
            String pkList = targetTable.columns().stream().filter(col -> col.isPrimaryKey())
                    .map(col -> "\"" + col.name() + "\"").collect(Collectors.joining(", "));
            String insertTemporalTableSQL = String.format(INSERT_TEMPORAL_TABLE_SQL, targetTable.parentSchema().name(),
                    targetTable.name(), getTemporalTableName(targetTable), pkList);
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
