package app.alegon.aws.sct.migrator.persistence.postgres;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.stream.Collectors;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.DataLoader;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;

@Component
public class PostgresDataLoader extends DataLoader {

    private final String POSTGRES_CONNECTION_STRING = "jdbc:postgresql://%s:%s/%s";
    private final String TABLE_COPY_COMMAND = "COPY \"%s\".%s (%s) FROM STDIN WITH (FORMAT CSV, HEADER true, DELIMITER '%s', NULL 'null')";

    public PostgresDataLoader(MigrationConfig migrationConfig) {
        super(migrationConfig);
    }

    @Override
    public void loadTable(String tableData, MigrationTable targetTable, MigrationDataSource migrationDataSource)
            throws DataLoaderException {
        String url = String.format(POSTGRES_CONNECTION_STRING, migrationDataSource.host(), migrationDataSource.port(),
                migrationDataSource.database());
        String user = migrationDataSource.userName();
        String password = migrationDataSource.password();

        try (Connection conn = DriverManager.getConnection(url, user, password);) {
            CopyManager copyManager = new CopyManager((BaseConnection) conn);
            StringReader reader = new StringReader(tableData);

            String columnList = targetTable.columns().stream().map(col -> "\"" + col.name() + "\"")
                    .collect(Collectors.joining(","));

            String copyQuery = String.format(TABLE_COPY_COMMAND, targetTable.parentSchema().name(), targetTable.name(),
                    columnList, migrationConfig.getCsvSeparator());

            copyManager.copyIn(copyQuery, reader);
        } catch (Exception e) {
            throw new DataLoaderException(e.getMessage(), e);
        }
    }

}
