package app.alegon.aws.sct.migrator.persistence.oracle;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationExtractorConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.model.MigrationTableColumn;
import app.alegon.aws.sct.migrator.persistence.exception.DataSerializeException;
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

    @Override
    public String serializeTableColumn(MigrationTableColumn migrationTableColumn, ResultSet resultSet)
            throws DataSerializeException {
        if (migrationTableColumn.dataType().equalsIgnoreCase("BLOB")) {
            try {
                Blob blob = resultSet.getBlob(migrationTableColumn.name());
                if (blob != null && blob.length() > 0) {
                    byte[] blobBytes = blob.getBytes(1, (int) blob.length());

                    StringBuilder hexString = new StringBuilder();
                    hexString.append("\\x");
                    for (byte b : blobBytes) {
                        hexString.append(String.format("%02X", b));
                    }

                    return hexString.toString();
                }

                return "";
            } catch (SQLException e) {
                throw new DataSerializeException(migrationTableColumn, e);
            }
        } else {
            return super.serializeTableColumn(migrationTableColumn, resultSet);
        }

    }
}
