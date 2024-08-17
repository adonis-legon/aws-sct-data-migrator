package app.alegon.aws.sct.migrator.model;

import java.util.List;

public record MigrationProject(String name, MigrationSchema sourceSchema, MigrationDataSource sourceDataSource,
                MigrationSchema targetSchema, MigrationDataSource targetDataSource,
                List<MigrationMapping> migrationMappings) {
}
