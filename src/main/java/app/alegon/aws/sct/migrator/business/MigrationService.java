package app.alegon.aws.sct.migrator.business;

import org.springframework.stereotype.Service;

import app.alegon.aws.sct.migrator.business.exception.DataMigrationException;
import app.alegon.aws.sct.migrator.config.MigrationObserverConfig;
import app.alegon.aws.sct.migrator.config.MigrationTransporterConfig;
import app.alegon.aws.sct.migrator.model.MigrationMapping;
import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.model.MigrationStatus;
import app.alegon.aws.sct.migrator.model.MigrationStatusType;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatus;
import app.alegon.aws.sct.migrator.model.MigrationMappingStatusType;
import app.alegon.aws.sct.migrator.persistence.DataExtractor;
import app.alegon.aws.sct.migrator.persistence.DataLoader;
import app.alegon.aws.sct.migrator.persistence.DataTransporter;
import app.alegon.aws.sct.migrator.persistence.exception.DataExtractorException;
import app.alegon.aws.sct.migrator.persistence.exception.DataLoaderException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterRemoveException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterSendException;
import app.alegon.aws.sct.migrator.util.BeanHelper;

@Service
public class MigrationService {

    private MigrationTransporterConfig migrationTransporterConfig;

    private MigrationObserverConfig migrationObserverConfig;

    private BeanHelper beanHelper;

    public MigrationService(MigrationTransporterConfig migrationTransporterConfig,
            MigrationObserverConfig migrationObserverConfig, BeanHelper beanHelper) {
        this.migrationTransporterConfig = migrationTransporterConfig;
        this.migrationObserverConfig = migrationObserverConfig;
        this.beanHelper = beanHelper;
    }

    public void migrate(MigrationProject migrationProject)
            throws DataMigrationException, DataTransporterSendException,
            DataLoaderException, DataTransporterRemoveException, DataExtractorException {

        MigrationObserver migrationObserver = (MigrationObserver) beanHelper
                .getBeanByName(migrationObserverConfig.getName());

        DataExtractor dataExtractor = (DataExtractor) beanHelper
                .getBeanByName(migrationProject.sourceDataSource().databaseEngine().getEngineName());
        dataExtractor.initialize(migrationProject.sourceDataSource());

        DataTransporter dataTransporter = (DataTransporter) beanHelper
                .getBeanByName(migrationTransporterConfig.getName());

        DataLoader dataLoader = (DataLoader) beanHelper
                .getBeanByName(migrationProject.targetDataSource().databaseEngine().getEngineName());
        dataLoader.initialize(migrationProject.targetDataSource());

        migrationObserver.onProjectMigrationStatus(
                new MigrationStatus(migrationProject, MigrationStatusType.InProgress, 0, null));

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

                migrationObserver.onTableMigrationStatus(
                        new MigrationMappingStatus(migrationMapping,
                                MigrationMappingStatusType.Successful, 100, null));
            } catch (Exception e) {
                migrationObserver.onTableMigrationStatus(
                        new MigrationMappingStatus(migrationMapping,
                                MigrationMappingStatusType.Failed, 100, e));
            } finally {
                dataTransporter.removeTableData(migrationMapping.sourceTable());
            }
        }

        dataExtractor.terminate();
        dataLoader.terminate();

        migrationObserver.onProjectMigrationStatus(
                new MigrationStatus(migrationProject, MigrationStatusType.Successful, 100, null));
    }
}
