package app.alegon.aws.sct.migrator.model;

import java.util.List;

public record MigrationSchema(String name, List<MigrationTable> migrationTables, boolean isSource) {
}
