package app.alegon.aws.sct.migrator.persistence.postgres;

import java.io.StringReader;
import java.util.stream.Collectors;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationLoaderConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;
import app.alegon.aws.sct.migrator.persistence.sql.SqlDataLoader;

@Component("POSTGRESQL")
public class PostgresDataLoader extends SqlDataLoader {

    private final String POSTGRES_CONNECTION_STRING = "jdbc:postgresql://%s:%s/%s";
    private final String TABLE_COPY_COMMAND = "COPY \"%s\".%s (%s) FROM STDIN WITH (FORMAT CSV, HEADER true, DELIMITER '%s', NULL 'null')";

    public PostgresDataLoader(MigrationLoaderConfig migrationLoaderConfig) {
        super(migrationLoaderConfig);
    }

    @Override
    public void loadTable(String tableData, MigrationTable targetTable, MigrationDataSource migrationDataSource)
            throws DataLoaderException {
        try {
            CopyManager copyManager = new CopyManager((BaseConnection) targetConnection);
            StringReader reader = new StringReader(tableData);

            String columnList = targetTable.columns().stream().map(col -> "\"" + col.name() + "\"")
                    .collect(Collectors.joining(","));

            String copyQuery = String.format(TABLE_COPY_COMMAND, targetTable.parentSchema().name(), targetTable.name(),
                    columnList, migrationLoaderConfig.getCsvSeparator());

            copyManager.copyIn(copyQuery, reader);
        } catch (Exception e) {
            throw new DataLoaderException(e.getMessage(), e);
        }
    }

    @Override
    protected String getConnectionString(MigrationDataSource migrationDataSource) {
        return String.format(POSTGRES_CONNECTION_STRING, migrationDataSource.host(), migrationDataSource.port(),
                migrationDataSource.database());
    }
}
