package app.alegon.aws.sct.migrator.business;

import org.springframework.stereotype.Service;

import app.alegon.aws.sct.migrator.business.exception.DataMigrationException;
import app.alegon.aws.sct.migrator.model.MigrationMapping;
import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.model.MigrationStatus;
import app.alegon.aws.sct.migrator.model.MigrationStatusType;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatus;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatusType;
import app.alegon.aws.sct.migrator.persistence.DataExtractor;
import app.alegon.aws.sct.migrator.persistence.DataLoader;
import app.alegon.aws.sct.migrator.persistence.DataTransporter;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterRemoveException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterSendException;

@Service
public class MigrationService {

        private DataExtractor dataExtractor;

        private DataTransporter dataTransporter;

        private DataLoader dataLoader;

        private MigrationObserver migrationObserver;

        public MigrationService(DataExtractor dataExtractor, DataLoader dataLoader, DataTransporter dataTransporter,
                        MigrationObserver migrationObserver) {
                this.dataExtractor = dataExtractor;
                this.dataLoader = dataLoader;
                this.dataTransporter = dataTransporter;
                this.migrationObserver = migrationObserver;
        }

        public void migrate(MigrationProject migrationProject)
                        throws DataMigrationException, DataTransporterSendException,
                        DataLoaderException, DataTransporterRemoveException {

                migrationObserver
                                .onProjectMigrationStatus(
                                                new MigrationStatus(migrationProject, MigrationStatusType.InProgress, 0,
                                                                null));

                for (MigrationMapping migrationMapping : migrationProject.migrationMappings()) {
                        migrationObserver.onTableMigrationStatus(
                                        new MigrationMappingStatus(migrationMapping, MigrationMappingStatusType.Started,
                                                        0, null));

                        try {
                                migrationObserver.onTableMigrationStatus(
                                                new MigrationMappingStatus(migrationMapping,
                                                                MigrationMappingStatusType.InProgress, 0, null));
                                String sourceTableData = dataExtractor.extractTable(migrationMapping.sourceTable(),
                                                migrationProject.sourceDataSource());

                                dataTransporter.receiveTableData(sourceTableData, migrationMapping.sourceTable());

                                String targetTableData = dataTransporter.sendTableData(migrationMapping.sourceTable());
                                dataLoader.loadTable(targetTableData, migrationMapping.targetTable(),
                                                migrationProject.targetDataSource());

                                dataTransporter.removeTableData(migrationMapping.sourceTable());

                                migrationObserver.onTableMigrationStatus(
                                                new MigrationMappingStatus(migrationMapping,
                                                                MigrationMappingStatusType.Successful, 100, null));
                        } catch (Exception e) {
                                migrationObserver.onTableMigrationStatus(
                                                new MigrationMappingStatus(migrationMapping,
                                                                MigrationMappingStatusType.Failed, 100, e));
                        }
                }

                migrationObserver
                                .onProjectMigrationStatus(
                                                new MigrationStatus(migrationProject, MigrationStatusType.Successful,
                                                                100, null));
        }
}
