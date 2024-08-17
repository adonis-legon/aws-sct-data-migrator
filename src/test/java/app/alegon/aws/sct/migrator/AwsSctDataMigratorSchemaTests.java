package app.alegon.aws.sct.migrator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.w3c.dom.Document;

import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.repository.ProjectRepository;
import app.alegon.aws.sct.migrator.util.XmlHelper;

@SpringBootTest
class AwsSctDataMigratorSchemaTests {

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private ProjectRepository projectRepository;

	@Test
	void whenLoadingMigrationProject_itShoulNotBeNull() throws Exception {
		Document awsSctProjectDocument = XmlHelper
				.loadDocumentFromResourceFile("AWS Schema Conversion Tool Project1.sct", resourceLoader);
		Document awsSctSourceSchemaDocument = XmlHelper
				.loadDocumentFromResourceFile("source_723d60f1-d894-4d4d-90e4-57a74ad23c79.xml", resourceLoader);
		Document awsSctTargetSchemaDocument = XmlHelper
				.loadDocumentFromResourceFile("target_102d257e-091c-400e-9b67-bd5fdc66fa27.xml", resourceLoader);

		MigrationProject migrationProject = projectRepository.loadFromXmlDocuments(awsSctProjectDocument,
				awsSctSourceSchemaDocument,
				awsSctTargetSchemaDocument);

		assertTrue(migrationProject != null && migrationProject.sourceSchema() != null);
		assertTrue(migrationProject.migrationMappings().size() == 11);

		List<String> expectedMigrationTablesOrdered = List.of("ARTIST", "EMPLOYEE", "GENRE", "MEDIATYPE",
				"PLAYLIST", "ALBUM", "CUSTOMER", "INVOICE", "TRACK", "INVOICELINE", "PLAYLISTTRACK");
		assertTrue(migrationProject.migrationMappings().stream().map(m -> m.sourceTable().name())
				.allMatch(s -> expectedMigrationTablesOrdered.stream().anyMatch(s2 -> s2.equalsIgnoreCase(s))));
	}

}
