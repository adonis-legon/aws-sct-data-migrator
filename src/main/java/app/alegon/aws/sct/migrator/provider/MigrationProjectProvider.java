package app.alegon.aws.sct.migrator.provider;

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
import app.alegon.aws.sct.migrator.provider.exception.MigrationSchemaInvalidFormatException;
import app.alegon.aws.sct.migrator.util.XmlHelper;
import app.alegon.aws.sct.migrator.util.resource.ResourceProvider;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderFactory;
import app.alegon.aws.sct.migrator.util.resource.ResourceProviderType;

@Repository
public class MigrationProjectProvider {
	private static final String PROJECT_NAME_XPATH = "/tree/instances/ProjectModel/@projectName";
	private static final String PROJECT_SOURCE_DATA_SOURCE_XPATH = "/tree/instances/ProjectModel/entities/sources/DbServer/ConnectionModel";
	private static final String PROJECT_TARGET_DATA_SOURCE_XPATH = "/tree/instances/ProjectModel/entities/targets/DbServer/ConnectionModel";
	private static final String PROJECT_MIGRATION_SOURCE_SCHEMA_XPATH = "/tree/instances/ProjectModel/relations/server-node-location/FullNameNodeInfoList/nameParts/FullNameNodeInfo[@typeNode='schema']";
	private static final String PROJECT_MIGRATION_SOURCE_DATABASE_XPATH = "/tree/instances/ProjectModel/relations/server-node-location/FullNameNodeInfoList/nameParts/FullNameNodeInfo[@typeNode='database']";
	private static final String PROJECT_MIGRATION_TARGET_SCHEMA_XPATH = "/tree/instances/ProjectModel/relations/server-node-location/related-locations/server-node-location/FullNameNodeInfoList/nameParts/FullNameNodeInfo[@typeNode='schema']";

	private static final String SCHEMAS_XPATH_TEMPLATE = "//category/schema[@name='%s']";
	private static final String SCHEMA_TABLES_XPATH = "./category/table";
	private static final String SCHEMA_TABLE_COLUMNS_XPATH = "./category/column";
	private static final String SCHEMA_TABLE_PK_CONSTRAINTS_XPATH = "./category/constraint[@constraint-type-desc='PRIMARY KEY']";
	private static final String SCHEMA_TABLE_RELATIONS_CONSTRAINTS_XPATH = "./category/constraint[@constraint-type-desc='FOREIGN KEY']/@referenced-table-name | ./category/constraint[@constraint-type-desc='FOREIGN KEY']/@r-table-name";

	private ResourceProviderFactory resourceProviderFactory;

	public MigrationProjectProvider(ResourceProviderFactory resourceProviderFactory) {
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
		NodeList projectNameNodeList = XmlHelper.getNodeList(awsSctProjectDocument, PROJECT_NAME_XPATH);
		if (projectNameNodeList == null || projectNameNodeList.getLength() == 0) {
			throw new Exception("Project name not found");
		}
		String projectName = projectNameNodeList.item(0).getTextContent();

		NodeList projectMigrationSourceSchemaNameNodeList = XmlHelper.getNodeList(awsSctProjectDocument,
				PROJECT_MIGRATION_SOURCE_SCHEMA_XPATH);
		if (projectMigrationSourceSchemaNameNodeList == null
				|| projectMigrationSourceSchemaNameNodeList.getLength() == 0) {
			throw new Exception("Project migration source schema name not found");
		}
		String projectMigrationSourceSchemaName = projectMigrationSourceSchemaNameNodeList.item(0).getAttributes()
				.getNamedItem("nameNode").getNodeValue();

		NodeList projectMigrationTargetSchemaNameNodeList = XmlHelper.getNodeList(awsSctProjectDocument,
				PROJECT_MIGRATION_TARGET_SCHEMA_XPATH);
		if (projectMigrationTargetSchemaNameNodeList == null
				|| projectMigrationTargetSchemaNameNodeList.getLength() == 0) {
			throw new Exception("Project migration target schema name not found");
		}
		String projectMigrationTargetSchemaName = projectMigrationTargetSchemaNameNodeList.item(0).getAttributes()
				.getNamedItem("nameNode").getNodeValue();

		MigrationSchema sourceSchema = loadSchemaFromXmlDocument(awsSctSourceSchemaDocument,
				projectMigrationSourceSchemaName, true);
		MigrationSchema targetSchema = loadSchemaFromXmlDocument(awsSctTargetSchemaDocument,
				projectMigrationTargetSchemaName, false);

		MigrationDataSource sourceDataSource = loadDataSourceFromXmlDocument(awsSctProjectDocument,
				dataSourceCredentials.sourcePassword(), sourceSchema);
		MigrationDataSource targetDataSource = loadDataSourceFromXmlDocument(awsSctProjectDocument,
				dataSourceCredentials.targetPassword(), targetSchema);

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

	private MigrationSchema loadSchemaFromXmlDocument(Document awsSctSchemaDocument, String mappingSchemaName,
			boolean isSource)
			throws XPathExpressionException, MigrationSchemaInvalidFormatException {
		String schemaNameXPathQuery = String.format(SCHEMAS_XPATH_TEMPLATE, mappingSchemaName);
		NodeList schemaNodeList = XmlHelper.getNodeList(awsSctSchemaDocument, schemaNameXPathQuery);
		if (schemaNodeList == null || schemaNodeList.getLength() == 0) {
			throw new MigrationSchemaInvalidFormatException(
					"Error reading schemas. Message: missing schemas in path " + schemaNameXPathQuery, null);
		}

		List<MigrationTable> migrationTables = new ArrayList<>();
		MigrationSchema migrationSchema = null;
		for (int i = 0; i < schemaNodeList.getLength(); i++) {
			Node schemaNode = schemaNodeList.item(i);
			migrationSchema = new MigrationSchema(schemaNode.getAttributes().getNamedItem("name").getNodeValue(),
					migrationTables, isSource);

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

					NodeList schemaTableRelationsContraintsNodeList = XmlHelper.getNodeList(schemaTableNode,
							SCHEMA_TABLE_RELATIONS_CONSTRAINTS_XPATH);
					for (int k = 0; k < schemaTableRelationsContraintsNodeList.getLength(); k++) {
						Node schemaTableRelationsContraintsNode = schemaTableRelationsContraintsNodeList
								.item(k);
						String tableNameDependency = schemaTableRelationsContraintsNode.getNodeValue();

						tableNameDependencies.add(tableNameDependency);
					}

					if (migrationTable.columns().isEmpty()) {
						throw new MigrationSchemaInvalidFormatException(
								"Error reading schemas. Message: missing columns in table " + migrationTable.name(),
								null);
					}

					migrationTables.add(migrationTable);
				}

				break;
			}
		}

		if (migrationSchema.migrationTables().isEmpty()) {
			throw new MigrationSchemaInvalidFormatException(
					"Error reading schemas. Message: missing tables in schema " + migrationSchema.name(), null);
		}

		return migrationSchema;
	}

	private MigrationDataSource loadDataSourceFromXmlDocument(Document awsSctProjectDocument,
			String dataSourcePassword, MigrationSchema schema) throws XPathExpressionException {

		String dataSourceXpathQUery = schema.isSource() ? PROJECT_SOURCE_DATA_SOURCE_XPATH
				: PROJECT_TARGET_DATA_SOURCE_XPATH;
		NodeList dataSourceNodeList = XmlHelper.getNodeList(awsSctProjectDocument, dataSourceXpathQUery);

		if (dataSourceNodeList != null && dataSourceNodeList.getLength() > 0) {
			Node dataSourceNode = dataSourceNodeList.item(0);
			DatabaseEngine dataSourceVendor = DatabaseEngine.valueOf(dataSourceNode
					.getAttributes().getNamedItem("vendor").getNodeValue());
			String dataSourceHost = dataSourceNode.getAttributes()
					.getNamedItem("serverName").getNodeValue();
			int dataSourcePort = Integer.parseInt(dataSourceNode.getAttributes()
					.getNamedItem("serverPort").getNodeValue());
			String dataSourceUser = dataSourceNode.getAttributes()
					.getNamedItem("userName").getNodeValue();
			String dataSourceDatabase = dataSourceNode.getAttributes()
					.getNamedItem("sid").getNodeValue();

			if (dataSourceDatabase == null || dataSourceDatabase.isEmpty()) {
				if (schema.isSource()) {
					NodeList dataSourceDatabaseNodeList = XmlHelper.getNodeList(awsSctProjectDocument,
							PROJECT_MIGRATION_SOURCE_DATABASE_XPATH);
					dataSourceDatabase = dataSourceDatabaseNodeList.getLength() > 0
							? dataSourceDatabaseNodeList.item(0).getAttributes().getNamedItem("nameNode").getNodeValue()
							: schema.name();
				} else {
					dataSourceDatabase = schema.name();
				}
			}

			return new MigrationDataSource(dataSourceVendor, dataSourceHost,
					dataSourcePort, dataSourceDatabase, dataSourceUser,
					dataSourcePassword, null);
		}

		return null;
	}

	private MigrationSchema getMigrationSchemaWithSortedTables(MigrationSchema migrationSchema) {
		Collections.sort(migrationSchema.migrationTables(), new Comparator<MigrationTable>() {
			@Override
			public int compare(MigrationTable migrationTable1, MigrationTable migrationTable2) {
				return migrationTable1.getDependencyCount() - migrationTable2.getDependencyCount();
			}
		});

		List<MigrationTable> visitedMigrationTables = new ArrayList<>();
		MigrationSchema orderedMigrationSchema = new MigrationSchema(migrationSchema.name(), visitedMigrationTables,
				migrationSchema.isSource());
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