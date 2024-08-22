package app.alegon.aws.sct.migrator.persistence.oracle;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationExtractorConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.sql.SqlDataExtractor;

@Component("ORACLE")
public class OracleDataExtractor extends SqlDataExtractor {

    private final String ORACLE_CONNECTION_STRING = "jdbc:oracle:thin:@//%s:%d/%s";
    private final String TABLE_SELECT_QUERY = "SELECT * FROM %s %sOFFSET %s ROWS FETCH FIRST %s ROWS ONLY";

    public OracleDataExtractor(MigrationExtractorConfig migrationExtractorConfig) {
        super(migrationExtractorConfig);
    }

    @Override
    protected String getConnectionString(MigrationDataSource migrationDataSource) {
        return String.format(ORACLE_CONNECTION_STRING, migrationDataSource.host(), migrationDataSource.port(),
                migrationDataSource.database());
    }

    @Override
    protected String getSelectQueryWithPagingTemplate(MigrationTable table, int offset, int pageSize) {
        return String.format(TABLE_SELECT_QUERY, table.name(), getOrderByPKColumnsQueryPart(table), offset, pageSize);
    }
}
