package app.alegon.aws.sct.migrator.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import app.alegon.aws.sct.migrator.business.MigrationService;
import app.alegon.aws.sct.migrator.config.MigrationServiceConfig;
import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.observer.LogMigratorObserver;
import app.alegon.aws.sct.migrator.provider.DataSourceCredentials;
import app.alegon.aws.sct.migrator.provider.MigrationProjectProvider;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderType;

public class AwsSctDataMigratorTask {

    private static final Logger logger = LoggerFactory.getLogger(LogMigratorObserver.class);

    private MigrationService migrationService;

    private MigrationProjectProvider migrationProjectProvider;

    private MigrationServiceConfig migrationServiceConfig;

    public AwsSctDataMigratorTask(MigrationService migrationService, MigrationProjectProvider migrationProjectProvider,
            MigrationServiceConfig migrationServiceConfig) {
        this.migrationService = migrationService;
        this.migrationProjectProvider = migrationProjectProvider;
        this.migrationServiceConfig = migrationServiceConfig;
    }

    @Scheduled(cron = "#{migrationServiceConfig.getSchedule()}", zone = "#{migrationServiceConfig.getTimezone()}")
    public void run() {
        try {
            MigrationProject migrationProject = migrationProjectProvider.loadFromPath(
                    migrationServiceConfig.getProjectPath(), ResourceProviderType.FileSystem, new DataSourceCredentials(
                            migrationServiceConfig.getSourcePassword(), migrationServiceConfig.getTargetPassword()));

            migrationService.migrate(migrationProject);
        } catch (Exception e) {
            logger.error("Error during migration", e);
        }
    }
}
