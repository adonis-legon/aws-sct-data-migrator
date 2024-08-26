package app.alegon.aws.sct.migrator.persistence.mssql;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationExtractorConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.sql.SqlDataExtractor;

@Component("MSSQL")
public class MssqlDataExtractor extends SqlDataExtractor {

    public MssqlDataExtractor(MigrationExtractorConfig migrationExtractorConfig) {
        super(migrationExtractorConfig);
    }

    private final String MSSQL_CONNECTION_STRING = "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false";
    private final String TABLE_SELECT_QUERY = "SELECT * FROM %s %sOFFSET %s ROWS FETCH NEXT %s ROWS ONLY";

    @Override
    protected String getConnectionString(MigrationDataSource migrationDataSource) {
        return String.format(MSSQL_CONNECTION_STRING, migrationDataSource.host(), migrationDataSource.port(),
                migrationDataSource.database());
    }

    @Override
    protected String getSelectQueryWithPagingTemplate(MigrationTable table, int offset, int pageSize) {
        return String.format(TABLE_SELECT_QUERY, table.name(), getOrderByPKColumnsQueryPart(table), offset, pageSize);
    }
}
