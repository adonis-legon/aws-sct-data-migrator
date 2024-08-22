package app.alegon.aws.sct.migrator;

import java.io.Console;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.business.MigrationService;
import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.repository.DataSourceCredentials;
import app.alegon.aws.sct.migrator.repository.MigrationProjectRepository;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderType;

@Component
@Profile("!test")
public class AwsSctDataMigratorConsole implements CommandLineRunner {

    private MigrationProjectRepository migrationProjectRepository;

    private MigrationService migrationService;

    private static final String PROJECT_PATH_ARG = "--project-path";

    public AwsSctDataMigratorConsole(MigrationService migrationService,
            MigrationProjectRepository migrationProjectRepository) {
        this.migrationService = migrationService;
        this.migrationProjectRepository = migrationProjectRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        String projectPath = parseProjectPath(args);
        if (projectPath == null) {
            System.err.println("Invalid arguments. Usage: java -jar <app.jar> --project-path <path>");
            System.exit(1);
        }

        Console console = System.console();
        if (console == null) {
            System.err.println("Unable to obtain console instance. Exiting.");
            System.exit(1);
        }

        char[] sourcePasswordChars = console.readPassword("Enter source password: ");
        String sourcePassword = new String(sourcePasswordChars);

        char[] targetPasswordChars = console.readPassword("Enter target password: ");
        String targetPassword = new String(targetPasswordChars);

        MigrationProject migrationProject = migrationProjectRepository.loadFromPath(projectPath,
                ResourceProviderType.FileSystem, new DataSourceCredentials(sourcePassword, targetPassword));

        migrationService.migrate(migrationProject);
    }

    private String parseProjectPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (PROJECT_PATH_ARG.equals(args[i]) && i < args.length - 1) {
                return args[i + 1];
            }
        }
        return null;
    }
}
