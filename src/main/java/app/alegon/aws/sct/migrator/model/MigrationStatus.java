package app.alegon.aws.sct.migrator.model;

public record MigrationStatus(MigrationProject migrationProject, MigrationStatusType migrationStatusType,
                float migrationPercentage, Object statusContext) {

}
