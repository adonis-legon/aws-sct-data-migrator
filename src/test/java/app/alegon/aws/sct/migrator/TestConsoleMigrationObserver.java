package app.alegon.aws.sct.migrator;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.business.MigrationObserver;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatus;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatusType;
import app.alegon.aws.sct.migrator.model.MigrationStatus;

@Component("TEST_CONSOLE")
public class TestConsoleMigrationObserver implements MigrationObserver {

    @Override
    public void onProjectMigrationStatus(MigrationStatus migrationStatus) {
        System.out.println(String.format("Migration project %s has status: %s",
                migrationStatus.migrationProject().name(),
                migrationStatus.migrationStatusType().getValue()));
    }

    @Override
    public void onTableMigrationStatus(MigrationMappingStatus migrationMappingStatus) {
        if (migrationMappingStatus.migrationStatusType() != MigrationMappingStatusType.Failed) {
            System.out.println(String.format(
                    "Migration from source table %s to target table %s, has status: %s and %s%% completed",
                    migrationMappingStatus.migrationMapping().sourceTable().name(),
                    migrationMappingStatus.migrationMapping().targetTable().name(),
                    migrationMappingStatus.migrationStatusType().getValue(),
                    Float.valueOf(migrationMappingStatus.migrationPercentage())));
        } else {
            System.out.println(String.format(
                    "Migration from source table %s to target table %s, has failed. Error: %s",
                    migrationMappingStatus.migrationMapping().sourceTable().name(),
                    migrationMappingStatus.migrationMapping().targetTable().name(),
                    ((Exception) migrationMappingStatus.statusContext()).getMessage()));
        }
    }
}
