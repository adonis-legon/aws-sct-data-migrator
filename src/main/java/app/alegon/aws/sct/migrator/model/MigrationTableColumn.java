package app.alegon.aws.sct.migrator.model;

public record MigrationTableColumn(String name, String dataType, boolean isPrimaryKey, MigrationTable parentTable) {

}
