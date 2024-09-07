package app.alegon.aws.sct.migrator.console;

import java.io.Console;

import org.springframework.boot.CommandLineRunner;

import app.alegon.aws.sct.migrator.business.MigrationService;
import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.provider.DataSourceCredentials;
import app.alegon.aws.sct.migrator.provider.MigrationProjectProvider;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderType;

public class AwsSctDataMigratorConsole implements CommandLineRunner {

    private MigrationProjectProvider migrationProjectProvider;

    private MigrationService migrationService;

    private static final String PROJECT_PATH_ARG = "--project-path";

    public AwsSctDataMigratorConsole(MigrationService migrationService,
            MigrationProjectProvider migrationProjectProvider) {
        this.migrationService = migrationService;
        this.migrationProjectProvider = migrationProjectProvider;
    }

    @Override
    public void run(String... args) throws Exception {
        String projectPath = parseProjectPath(args);
        if (projectPath == null) {
            System.err.println("Invalid arguments. Usage: java -jar <app.jar> --project-path=<path>");
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

        MigrationProject migrationProject = migrationProjectProvider.loadFromPath(projectPath,
                ResourceProviderType.FileSystem, new DataSourceCredentials(sourcePassword, targetPassword));

        migrationService.migrate(migrationProject);
    }

    private String parseProjectPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String[] argParts = args[i].split("=");
            if (argParts.length >= 2 && PROJECT_PATH_ARG.equals(argParts[0])) {
                return argParts[1];
            }
        }
        return null;
    }
}
