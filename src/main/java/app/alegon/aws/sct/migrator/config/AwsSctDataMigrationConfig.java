package app.alegon.aws.sct.migrator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import app.alegon.aws.sct.migrator.business.MigrationService;
import app.alegon.aws.sct.migrator.console.AwsSctDataMigratorConsole;
import app.alegon.aws.sct.migrator.provider.MigrationProjectProvider;
import app.alegon.aws.sct.migrator.task.AwsSctDataMigratorTask;

@Configuration
public class AwsSctDataMigrationConfig {

    @Autowired
    private MigrationService migrationService;

    @Autowired
    private MigrationProjectProvider migrationProjectProvider;

    @Autowired
    private MigrationServiceConfig migrationServiceConfig;

    @Bean
    @ConditionalOnProperty(name = "mode", havingValue = "console")
    public AwsSctDataMigratorConsole consoleRunner() {
        return new AwsSctDataMigratorConsole(migrationService, migrationProjectProvider);
    }

    @Bean
    @ConditionalOnProperty(name = "mode", havingValue = "service")
    public AwsSctDataMigratorTask taskService() {
        return new AwsSctDataMigratorTask(migrationService, migrationProjectProvider, migrationServiceConfig);
    }
}
