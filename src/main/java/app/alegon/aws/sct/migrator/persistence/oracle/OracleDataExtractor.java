package app.alegon.aws.sct.migrator.persistence.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.MigrationConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.DataExtractor;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;

@Component
public class OracleDataExtractor extends DataExtractor {

    private final String ORACLE_CONNECTION_STRING = "jdbc:oracle:thin:@//%s:%d/%s";
    private final String TABLE_SELECT_QUERY = "SELECT * FROM %s";

    public OracleDataExtractor(MigrationConfig migrationConfig) {
        super(migrationConfig);
    }

    @Override
    public String extractTable(MigrationTable table, MigrationDataSource migrationDataSource)
            throws DataExtractorException {
        String url = String.format(ORACLE_CONNECTION_STRING, migrationDataSource.host(), migrationDataSource.port(),
                migrationDataSource.database());
        String username = migrationDataSource.userName();
        String password = migrationDataSource.password();
        String tableName = table.name();

        StringBuilder outputContent = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(url, username, password);
                Statement stmt = conn.createStatement();) {

            String query = String.format(TABLE_SELECT_QUERY, tableName);
            ResultSet rs = stmt.executeQuery(query);

            // Write column headers
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                outputContent.append(metaData.getColumnName(i));
                if (i != columnCount) {
                    outputContent.append(migrationConfig.getCsvSeparator());
                }
            }
            outputContent.append("\n");

            // Write data rows
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    outputContent.append(rs.getString(i));
                    if (i != columnCount) {
                        outputContent.append(migrationConfig.getCsvSeparator());
                    }
                }
                outputContent.append("\n");
            }

            return outputContent.toString();
        } catch (SQLException e) {
            throw new DataExtractorException(e.getMessage(), e);
        }
    }

}
