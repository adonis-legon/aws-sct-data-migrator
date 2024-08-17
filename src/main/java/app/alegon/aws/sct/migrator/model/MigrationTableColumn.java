package app.alegon.aws.sct.migrator.model;

public record MigrationTableColumn(String name, String dataType, MigrationTable parentTable) {

}
