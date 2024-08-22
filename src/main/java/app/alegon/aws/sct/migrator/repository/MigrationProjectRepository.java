package app.alegon.aws.sct.migrator.repository;

import java.io.IOException;
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
import app.alegon.aws.sct.migrator.util.resource.ResourceProvider;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderFactory;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderType;

@Repository
public class MigrationProjectRepository {
	private static final String PROJECT_NAME_XPATH = "/tree/instances/ProjectModel/@projectName";
	private static final String PROJECT_SOURCE_DATA_SOURCE_XPATH = "/tree/instances/ProjectModel/entities/sources/DbServer/ConnectionModel";
	private static final String PROJECT_TARGET_DATA_SOURCE_XPATH = "/tree/instances/ProjectModel/entities/targets/DbServer/ConnectionModel";

	private static final String SCHEMAS_XPATH = "/tree/metadata/category/schema[@is-empty='N']";
	private static final String SCHEMA_TABLES_XPATH = "./category/table";
	private static final String SCHEMA_TABLE_COLUMNS_XPATH = "./category/column";
	private static final String SCHEMA_TABLE_FK_CONSTRAINTS_XPATH = "./category/constraint[@constraint-type='R']";
	private static final String SCHEMA_TABLE_PK_CONSTRAINTS_XPATH = "./category/constraint[@constraint-type='P']";

	private ResourceProviderFactory resourceProviderFactory;

	public MigrationProjectRepository(ResourceProviderFactory resourceProviderFactory) {
		this.resourceProviderFactory = resourceProviderFactory;
	}

	public MigrationProject loadFromPath(String awsSctProjectPath, ResourceProviderType resourceProviderType,
			DataSourceCredentials dataSourceCredentials) throws Exception {
		ResourceProvider resourceProvider = resourceProviderFactory.getResourceProvider(resourceProviderType,
				awsSctProjectPath);
		String awsSctProjectFileContent = resourceProvider.getResourceContent(resourceProvider.getProjectFile());

		Document awsSctProjectDocument = XmlHelper.loadDocumentFromString(awsSctProjectFileContent);
		Document awsSctSourceSchemaDocument = getSchemaDocument(awsSctProjectDocument, PROJECT_SOURCE_DATA_SOURCE_XPATH,
				resourceProvider, "source");
		Document awsSctTargetSchemaDocument = getSchemaDocument(awsSctProjectDocument, PROJECT_TARGET_DATA_SOURCE_XPATH,
				resourceProvider, "target");

		return loadFromXmlDocuments(awsSctProjectDocument, awsSctSourceSchemaDocument, awsSctTargetSchemaDocument,
				dataSourceCredentials);
	}

	private Document getSchemaDocument(Document awsSctProjectDocument, String projectSchemaXpathQuery,
			ResourceProvider resourceProvider, String schemaFilePrefix) throws IOException, Exception {
		NodeList dataSourceNodeList = XmlHelper.getNodeList(awsSctProjectDocument, projectSchemaXpathQuery);
		if (dataSourceNodeList.getLength() == 0) {
			throw new Exception("Data source not found");
		}

		String schemaId = dataSourceNodeList.item(0).getParentNode().getAttributes().getNamedItem("metaStorageUuid")
				.getNodeValue();
		String schemaFileName = String.format("%s_%s.xml", schemaFilePrefix, schemaId);

		return XmlHelper.loadDocumentFromString(resourceProvider.getResourceContent(schemaFileName));
	}

	private MigrationProject loadFromXmlDocuments(Document awsSctProjectDocument, Document awsSctSourceSchemaDocument,
			Document awsSctTargetSchemaDocument, DataSourceCredentials dataSourceCredentials) throws Exception {
		String projectName = XmlHelper.getNodeList(awsSctProjectDocument, PROJECT_NAME_XPATH).item(0)
				.getTextContent();
		MigrationSchema sourceSchema = loadSchemaFromXmlDocument(awsSctSourceSchemaDocument);
		MigrationSchema targetSchema = loadSchemaFromXmlDocument(awsSctTargetSchemaDocument);

		MigrationDataSource sourceDataSource = loadDataSourceFromXmlDocument(awsSctProjectDocument,
				PROJECT_SOURCE_DATA_SOURCE_XPATH, dataSourceCredentials.sourcePassword());
		MigrationDataSource targetDataSource = loadDataSourceFromXmlDocument(awsSctProjectDocument,
				PROJECT_TARGET_DATA_SOURCE_XPATH, dataSourceCredentials.targetPassword());

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

	private MigrationDataSource loadDataSourceFromXmlDocument(Document awsSctProjectDocument,
			String dataSourceXpathQUery, String dataSourcePassword) throws XPathExpressionException {

		NodeList dataSourceNodeList = XmlHelper.getNodeList(awsSctProjectDocument, dataSourceXpathQUery);

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
					dataSourcePassword, null);
		}

		return null;
	}

	private MigrationSchema loadSchemaFromXmlDocument(Document awsSctSchemaDocument)
			throws XPathExpressionException {
		NodeList schemaNodeList = XmlHelper.getNodeList(awsSctSchemaDocument, SCHEMAS_XPATH);

		List<MigrationTable> migrationTables = new ArrayList<>();
		MigrationSchema migrationSchema = null;

		if (schemaNodeList != null && schemaNodeList.getLength() > 0) {
			for (int i = 0; i < schemaNodeList.getLength(); i++) {
				Node schemaNode = schemaNodeList.item(i);
				migrationSchema = new MigrationSchema(schemaNode.getAttributes().getNamedItem("name").getNodeValue(),
						migrationTables);

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

						List<String> tablePKColumnsName = new ArrayList<>();
						NodeList schemaTablePkConstraintNodeList = XmlHelper.getNodeList(schemaTableNode,
								SCHEMA_TABLE_PK_CONSTRAINTS_XPATH);
						if (schemaTablePkConstraintNodeList.getLength() > 0) {
							NodeList schemaTablePkConstraintColumnNodeList = XmlHelper
									.getNodeList(schemaTablePkConstraintNodeList.item(0), SCHEMA_TABLE_COLUMNS_XPATH);
							for (int k = 0; k < schemaTablePkConstraintColumnNodeList.getLength(); k++) {
								tablePKColumnsName.add(schemaTablePkConstraintColumnNodeList.item(k).getAttributes()
										.getNamedItem("name").getNodeValue());
							}
						}

						NodeList schemaTableColumnNodeList = XmlHelper.getNodeList(
								schemaTableNode, SCHEMA_TABLE_COLUMNS_XPATH);
						for (int k = 0; k < schemaTableColumnNodeList.getLength(); k++) {
							Node schemaTableColumnNode = schemaTableColumnNodeList.item(k);
							String columnName = schemaTableColumnNode.getAttributes()
									.getNamedItem("name").getNodeValue();
							String columnType = schemaTableColumnNode.getAttributes()
									.getNamedItem("dt-name").getNodeValue();

							boolean isPK = tablePKColumnsName.contains(columnName);
							tableColumns.add(new MigrationTableColumn(columnName, columnType, isPK, migrationTable));
						}

						NodeList schemaTableFKConstraintNodeList = XmlHelper.getNodeList(schemaTableNode,
								SCHEMA_TABLE_FK_CONSTRAINTS_XPATH);
						for (int k = 0; k < schemaTableFKConstraintNodeList.getLength(); k++) {
							Node schemaTableColumnConstraintNode = schemaTableFKConstraintNodeList
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
		}

		return migrationSchema;
	}

	private MigrationSchema getMigrationSchemaWithSortedTables(MigrationSchema migrationSchema) {
		Collections.sort(migrationSchema.migrationTables(), new Comparator<MigrationTable>() {
			@Override
			public int compare(MigrationTable migrationTable1, MigrationTable migrationTable2) {
				return migrationTable1.getDependencyCount() - migrationTable2.getDependencyCount();
			}
		});

		List<MigrationTable> visitedMigrationTables = new ArrayList<>();
		MigrationSchema orderedMigrationSchema = new MigrationSchema(migrationSchema.name(), visitedMigrationTables);
		Queue<MigrationTable> processingMigrationTables = new LinkedList<>(migrationSchema.migrationTables());

		while (processingMigrationTables.size() > 0) {
			MigrationTable currentMigrationTable = processingMigrationTables.poll();

			if (currentMigrationTable.getDependencyCount() == 0 || currentMigrationTable.tableNameDependencies()
					.stream().allMatch(tableName -> visitedMigrationTables.stream().map(t -> t.name())
							.anyMatch(v -> v.equalsIgnoreCase(tableName)))) {
				MigrationTable orderedMigrationTable = new MigrationTable(currentMigrationTable.name(),
						currentMigrationTable.columns(), currentMigrationTable.tableNameDependencies(),
						orderedMigrationSchema);
				visitedMigrationTables.add(orderedMigrationTable);
			} else {
				processingMigrationTables.add(currentMigrationTable);
			}
		}

		return orderedMigrationSchema;
	}
}