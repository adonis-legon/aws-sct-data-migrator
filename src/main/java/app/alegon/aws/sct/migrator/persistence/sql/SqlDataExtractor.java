package app.alegon.aws.sct.migrator.persistence.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import app.alegon.aws.sct.migrator.config.MigrationExtractorConfig;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.model.MigrationTableColumn;
import app.alegon.aws.sct.migrator.persistence.DataExtractor;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;
import app.alegon.aws.sct.migrator.persistence.exception.DataSerializeException;

public abstract class SqlDataExtractor extends DataExtractor {

    protected Connection sourceConnection;

    protected boolean isInitialized;

    private final String PK_BASED_QUERY_PART = "ORDER BY %s ";

    public SqlDataExtractor(MigrationExtractorConfig migrationExtractorConfig) {
        super(migrationExtractorConfig);
    }

    @Override
    public void initialize(MigrationDataSource migrationDataSource) throws DataExtractorException {
        try {
            if (sourceConnection == null || sourceConnection.isClosed()) {
                sourceConnection = DriverManager.getConnection(getConnectionString(migrationDataSource),
                        migrationDataSource.userName(), migrationDataSource.password());
                isInitialized = true;
            }
        } catch (SQLException e) {
            throw new DataExtractorException("Error connecting to source datasource. Message: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public String extractTable(MigrationTable table, MigrationDataSource migrationDataSource)
            throws DataExtractorException, DataSerializeException {

        if (!isInitialized) {
            throw new DataExtractorException("Data Extractor is not initialized", null);
        }

        StringBuilder outputContent = new StringBuilder();
        try (Statement stmt = sourceConnection.createStatement();) {

            outputContent.append(table.columns().stream().map(col -> "\"" + col.name() + "\"")
                    .collect(Collectors.joining(migrationExtractorConfig.getCsvSeparator())) + "\n");

            int offset = 0;
            do {
                SelectQueryParameters selectQueryParameters = new SelectQueryParameters(offset, getPagingSize());
                String query = getSelectQueryAsTemplate(table, selectQueryParameters);
                ResultSet rs = stmt.executeQuery(query);
                if (!rs.next())
                    break;

                do {
                    List<String> rowDataList = new ArrayList<>();
                    for (MigrationTableColumn column : table.columns()) {
                        rowDataList.add(serializeTableColumn(column, rs));
                    }
                    outputContent.append(
                            rowDataList.stream().collect(Collectors.joining(migrationExtractorConfig.getCsvSeparator()))
                                    + "\n");
                } while (rs.next());

                offset += getPagingSize();

            } while (true);

            return outputContent.toString();
        } catch (SQLException e) {
            throw new DataExtractorException(e.getMessage(), e);
        }
    }

    protected String serializeTableColumn(MigrationTableColumn migrationTableColumn, ResultSet resultSet)
            throws DataSerializeException {
        try {
            String columnValue = resultSet.getString(migrationTableColumn.name());
            return columnValue != null ? columnValue.replaceAll("[\t\n]", " ") : columnValue;
        } catch (SQLException e) {
            throw new DataSerializeException(migrationTableColumn, e);
        }
    }

    @Override
    public void terminate() throws DataExtractorException {
        try {
            if (sourceConnection != null && !sourceConnection.isClosed()) {
                sourceConnection.close();
            }
        } catch (SQLException e) {
            throw new DataExtractorException(
                    "Error closing connection to source datasource. Message: " + e.getMessage(), e);
        }
    }

    protected String getOrderByPKColumnsQueryPart(MigrationTable table) {
        String orderByQueryPart = "";
        List<MigrationTableColumn> tablePkList = table.getPrimaryKeyColumns();
        if (tablePkList.size() > 0) {
            orderByQueryPart = String.format(PK_BASED_QUERY_PART,
                    tablePkList.stream().map(c -> c.name()).collect(Collectors.joining(",")));
        }

        return orderByQueryPart;
    }

    protected int getPagingSize() {
        return migrationExtractorConfig.getPageSize();
    }

    protected abstract String getConnectionString(MigrationDataSource migrationDataSource);

    protected abstract String getSelectQueryAsTemplate(MigrationTable table,
            SelectQueryParameters selectQueryParameters);
}
