package app.alegon.aws.sct.migrator.model;

import java.util.Map;

public record MigrationDataSource(DatabaseEngine databaseEngine, String host, int port, String database,
                String userName, String password, Map<String, String> additionalConfig) {

}
