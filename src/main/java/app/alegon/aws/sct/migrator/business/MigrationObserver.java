package app.alegon.aws.sct.migrator.business;

import app.alegon.aws.sct.migrator.model.MigrationMappingStatus;
import app.alegon.aws.sct.migrator.model.MigrationStatus;

public interface MigrationObserver {

    public void onProjectMigrationStatus(MigrationStatus migrationStatus);

    public void onTableMigrationStatus(MigrationMappingStatus migrationMappingStatus);
}
