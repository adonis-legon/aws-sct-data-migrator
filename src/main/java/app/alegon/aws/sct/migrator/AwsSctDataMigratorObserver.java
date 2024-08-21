package app.alegon.aws.sct.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.alegon.aws.sct.migrator.business.MigrationObserver;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatus;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatusType;
import app.alegon.aws.sct.migrator.model.MigrationStatus;

public class AwsSctDataMigratorObserver implements MigrationObserver {
    private static final Logger logger = LoggerFactory.getLogger(AwsSctDataMigratorObserver.class);

    @Override
    public void onProjectMigrationStatus(MigrationStatus migrationStatus) {
        logger.info(String.format("Migration project %s has status: %s",
                migrationStatus.migrationProject().name(),
                migrationStatus.migrationStatusType().getValue()));
    }

    @Override
    public void onTableMigrationStatus(MigrationMappingStatus migrationMappingStatus) {
        if (migrationMappingStatus.migrationStatusType() != MigrationMappingStatusType.Failed) {
            logger.info(String.format(
                    "Migration from source table %s to target table %s, has status: %s and %s%% completed",
                    migrationMappingStatus.migrationMapping().sourceTable().name(),
                    migrationMappingStatus.migrationMapping().targetTable().name(),
                    migrationMappingStatus.migrationStatusType().getValue(),
                    Float.valueOf(migrationMappingStatus.migrationPercentage())));
        } else {
            logger.error(String.format(
                    "Migration from source table %s to target table %s, has failed. Error: %s",
                    migrationMappingStatus.migrationMapping().sourceTable().name(),
                    migrationMappingStatus.migrationMapping().targetTable().name(),
                    ((Exception) migrationMappingStatus.statusContext()).getMessage()));
        }

    }

}
