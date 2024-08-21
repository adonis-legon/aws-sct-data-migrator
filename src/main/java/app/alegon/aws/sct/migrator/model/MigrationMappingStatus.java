package app.alegon.aws.sct.migrator.model;

public record MigrationMappingStatus(MigrationMapping migrationMapping, MigrationMappingStatusType migrationStatusType,
                float migrationPercentage, Object statusContext) {

}
