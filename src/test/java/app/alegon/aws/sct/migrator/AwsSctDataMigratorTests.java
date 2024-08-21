package app.alegon.aws.sct.migrator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import app.alegon.aws.sct.migrator.business.MigrationService;
import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.repository.DataSourceCredentials;
import app.alegon.aws.sct.migrator.repository.MigrationProjectRepository;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderType;

@SpringBootTest
@ActiveProfiles("test")
class AwsSctDataMigratorTests {

	@Autowired
	private MigrationProjectRepository projectRepository;

	@Autowired
	private MigrationService migrationService;

	@Test
	void whenLoadingMigrationProject_itShoulNotBeNull() throws Exception {
		MigrationProject migrationProject = projectRepository.loadFromPath("projects/demo-chinook-oracle-to-postgres",
				ResourceProviderType.ApplicationResource, new DataSourceCredentials("", ""));

		assertTrue(migrationProject != null && migrationProject.sourceSchema() != null);
		assertTrue(migrationProject.migrationMappings().size() == 11);

		List<String> expectedMigrationTablesOrdered = List.of("ARTIST", "EMPLOYEE", "GENRE", "MEDIATYPE",
				"PLAYLIST", "ALBUM", "CUSTOMER", "INVOICE", "TRACK", "INVOICELINE", "PLAYLISTTRACK");
		assertTrue(migrationProject.migrationMappings().stream().map(m -> m.sourceTable().name())
				.allMatch(s -> expectedMigrationTablesOrdered.stream().anyMatch(s2 -> s2.equalsIgnoreCase(s))));
	}

	@Test
	void whenMigratingData_itShouldNotThrowError() {
		try {
			MigrationProject migrationProject = projectRepository.loadFromPath(
					"projects/demo-chinook-oracle-to-postgres", ResourceProviderType.ApplicationResource,
					new DataSourceCredentials("c##chinook", "c##chinook"));

			migrationService.migrate(migrationProject);
		} catch (Exception e) {
			fail("Exception should not have been thrown: " + e.getMessage());
		}
	}
}
