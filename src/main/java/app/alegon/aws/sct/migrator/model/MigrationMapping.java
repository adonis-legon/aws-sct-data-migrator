package app.alegon.aws.sct.migrator.model;

public record MigrationMapping(MigrationTable sourceTable, MigrationTable targetTable, int migrationOrder) {

}
