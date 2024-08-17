package app.alegon.aws.sct.migrator.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import javax.xml.xpath.XPathExpressionException;

import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import app.alegon.aws.sct.migrator.model.DatabaseEngine;
import app.alegon.aws.sct.migrator.model.MigrationDataSource;
import app.alegon.aws.sct.migrator.model.MigrationMapping;
import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.model.MigrationSchema;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.model.MigrationTableColumn;
import app.alegon.aws.sct.migrator.util.XmlHelper;

@Repository
public class ProjectRepository {
	private static final String PROJECT_NAME_XPATH = "/tree/instances/ProjectModel/@projectName";
	private static final String PROJECT_SOURCE_SCHEMA_ID_XPATH = "/tree/instances/ProjectModel/entities/sources/DbServer/@metaStorageUuid";
	private static final String PROJECT_SOURCE_DATA_SOURCE_XPATH = "/tree/instances/ProjectModel/entities/sources/DbServer/ConnectionModel";
	private static final String PROJECT_TARGET_SCHEMA_ID_XPATH = "/tree/instances/ProjectModel/entities/targets/DbServer/@metaStorageUuid";
	private static final String PROJECT_TARGET_DATA_SOURCE_XPATH = "/tree/instances/ProjectModel/entities/targets/DbServer/ConnectionModel";

	private static final String SCHEMA_NAME_XPATH = "/tree/metadata/@name";
	private static final String SCHEMAS_XPATH = "/tree/metadata/category/schema[@is-empty='N']";
	private static final String SCHEMA_TABLES_XPATH = "./category/table";
	private static final String SCHEMA_TABLE_COLUMNS_XPATH = "./category/column";
	private static final String SCHEMA_TABLE_CONSTRAINTS_XPATH = "./category/constraint[@constraint-type='R']";

	public MigrationProject loadFromPath(String awsSctProjectPath) throws Exception {
		Path firstSctFile = Files.walk(Paths.get(awsSctProjectPath))
				.filter(path -> path.toString().endsWith(".sct"))
				.findFirst()
				.orElse(null);

		if (firstSctFile == null) {
			throw new Exception("No .sct file found in the given path.");
		}

		Document awsSctProjectDocument = XmlHelper.loadDocumentFromString(Files.readString(firstSctFile));

		Node sourceSchemaIdNode = XmlHelper.getNodeList(awsSctProjectDocument, PROJECT_SOURCE_SCHEMA_ID_XPATH)
				.item(0);
		Document awsSctSourceSchemaDocument = XmlHelper
				.loadDocumentFromString(Files.readString(Paths.get(awsSctProjectPath,
						String.format("source_%s.xml", sourceSchemaIdNode.getTextContent()))));

		Node targetSchemaIdNode = XmlHelper.getNodeList(awsSctProjectDocument, PROJECT_TARGET_SCHEMA_ID_XPATH)
				.item(0);
		Document awsSctTargetSchemaDocument = XmlHelper
				.loadDocumentFromString(Files.readString(Paths.get(awsSctProjectPath,
						String.format("target_%s.xml", targetSchemaIdNode.getTextContent()))));

		return loadFromXmlDocuments(awsSctProjectDocument, awsSctSourceSchemaDocument,
				awsSctTargetSchemaDocument);
	}

	public MigrationProject loadFromXmlDocuments(Document awsSctProjectDocument,
			Document awsSctSourceSchemaDocument, Document awsSctTargetSchemaDocument) throws Exception {
		String projectName = XmlHelper.getNodeList(awsSctProjectDocument, PROJECT_NAME_XPATH).item(0)
				.getTextContent();
		MigrationSchema sourceSchema = loadSchemaFromXmlDocument(awsSctSourceSchemaDocument);
		MigrationSchema targetSchema = loadSchemaFromXmlDocument(awsSctTargetSchemaDocument);

		MigrationDataSource sourceDataSource = loadDataSourceFromXmlDocument(awsSctProjectDocument, true);
		MigrationDataSource targetDataSource = loadDataSourceFromXmlDocument(awsSctProjectDocument, false);

		MigrationSchema sortedSourceSchema = getMigrationSchemaWithSortedTables(sourceSchema);

		List<MigrationMapping> migrationMappings = new ArrayList<>();
		int order = 1;

		for (MigrationTable sourceMigrationTable : sortedSourceSchema.migrationTables()) {
			for (MigrationTable targetMigrationTable : targetSchema.migrationTables()) {
				if (targetMigrationTable.name().equalsIgnoreCase(sourceMigrationTable.name())) {
					migrationMappings.add(new MigrationMapping(sourceMigrationTable,
							targetMigrationTable, order++));
					break;
				}
			}
		}

		return new MigrationProject(projectName, sortedSourceSchema, sourceDataSource, targetSchema, targetDataSource,
				migrationMappings);
	}

	public MigrationDataSource loadDataSourceFromXmlDocument(Document awsSctProjectDocument, boolean isSource)
			throws XPathExpressionException {

		NodeList dataSourceNodeList = XmlHelper.getNodeList(awsSctProjectDocument,
				isSource ? PROJECT_SOURCE_DATA_SOURCE_XPATH : PROJECT_TARGET_DATA_SOURCE_XPATH);

		if (dataSourceNodeList != null && dataSourceNodeList.getLength() > 0) {
			Node dataSourceNode = dataSourceNodeList.item(0);
			DatabaseEngine dataSourceVendor = DatabaseEngine.valueOf(dataSourceNode
					.getAttributes().getNamedItem("vendor").getNodeValue());
			String dataSourceHost = dataSourceNode.getAttributes()
					.getNamedItem("serverName").getNodeValue();
			int dataSourcePort = Integer.parseInt(dataSourceNode.getAttributes()
					.getNamedItem("serverPort").getNodeValue());
			String dataSourceDatabase = dataSourceNode.getAttributes()
					.getNamedItem("sid").getNodeValue();
			String dataSourceUser = dataSourceNode.getAttributes()
					.getNamedItem("userName").getNodeValue();

			return new MigrationDataSource(dataSourceVendor, dataSourceHost,
					dataSourcePort, dataSourceDatabase, dataSourceUser,
					null, null);
		}

		return null;
	}

	public MigrationSchema loadSchemaFromXmlDocument(Document awsSctSchemaDocument)
			throws XPathExpressionException {
		Node schemaNameNode = XmlHelper.getNodeList(awsSctSchemaDocument, SCHEMA_NAME_XPATH).item(0);
		NodeList schemaNodeList = XmlHelper.getNodeList(awsSctSchemaDocument, SCHEMAS_XPATH);

		List<MigrationTable> migrationTables = new ArrayList<>();
		MigrationSchema migrationSchema = null;

		if (schemaNodeList != null && schemaNodeList.getLength() > 0) {
			for (int i = 0; i < schemaNodeList.getLength(); i++) {
				Node schemaNode = schemaNodeList.item(i);
				NodeList schemaTableNodeList = XmlHelper.getNodeList(schemaNode, SCHEMA_TABLES_XPATH);

				if (schemaTableNodeList.getLength() > 0) {
					for (int j = 0; j < schemaTableNodeList.getLength(); j++) {
						Node schemaTableNode = schemaTableNodeList.item(j);
						String tableName = schemaTableNode.getAttributes().getNamedItem("name")
								.getNodeValue();

						List<MigrationTableColumn> tableColumns = new ArrayList<>();
						List<String> tableNameDependencies = new ArrayList<>();
						MigrationTable migrationTable = new MigrationTable(tableName, tableColumns,
								tableNameDependencies,
								migrationSchema);

						NodeList schemaTableColumnNodeList = XmlHelper.getNodeList(
								schemaTableNode, SCHEMA_TABLE_COLUMNS_XPATH);
						for (int k = 0; k < schemaTableColumnNodeList.getLength(); k++) {
							Node schemaTableColumnNode = schemaTableColumnNodeList.item(k);
							String columnName = schemaTableColumnNode.getAttributes()
									.getNamedItem("name").getNodeValue();
							String columnType = schemaTableColumnNode.getAttributes()
									.getNamedItem("data-type").getNodeValue();

							tableColumns.add(new MigrationTableColumn(columnName, columnType,
									migrationTable));
						}

						NodeList schemaTableConstraintNodeList = XmlHelper.getNodeList(schemaTableNode,
								SCHEMA_TABLE_CONSTRAINTS_XPATH);
						for (int k = 0; k < schemaTableConstraintNodeList.getLength(); k++) {
							Node schemaTableColumnConstraintNode = schemaTableConstraintNodeList
									.item(k);
							String tableNameDependency = schemaTableColumnConstraintNode
									.getAttributes()
									.getNamedItem("r-table-name").getNodeValue();

							tableNameDependencies.add(tableNameDependency);
						}

						migrationTables.add(migrationTable);
					}

					break;
				}
			}

			migrationSchema = new MigrationSchema(schemaNameNode.getTextContent(), migrationTables);
		}

		return migrationSchema;
	}

	public MigrationSchema getMigrationSchemaWithSortedTables(MigrationSchema migrationSchema) {
		Collections.sort(migrationSchema.migrationTables(), new Comparator<MigrationTable>() {
			@Override
			public int compare(MigrationTable migrationTable1, MigrationTable migrationTable2) {
				return migrationTable1.getDependencyCount() - migrationTable2.getDependencyCount();
			}
		});

		List<MigrationTable> visitedMigrationTables = new ArrayList<>();
		Queue<MigrationTable> processingMigrationTables = new LinkedList<>(migrationSchema.migrationTables());

		while (processingMigrationTables.size() > 0) {
			MigrationTable currentMigrationTable = processingMigrationTables.poll();

			if (currentMigrationTable.getDependencyCount() == 0 || currentMigrationTable.tableNameDependencies()
					.stream().allMatch(tableName -> visitedMigrationTables.stream().map(t -> t.name())
							.anyMatch(v -> v.equalsIgnoreCase(tableName)))) {
				visitedMigrationTables.add(currentMigrationTable);
			} else {
				processingMigrationTables.add(currentMigrationTable);
			}
		}

		return new MigrationSchema(migrationSchema.name(), visitedMigrationTables);
	}
}