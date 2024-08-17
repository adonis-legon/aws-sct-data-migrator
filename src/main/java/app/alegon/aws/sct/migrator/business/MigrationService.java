package app.alegon.aws.sct.migrator.business;

import org.springframework.stereotype.Service;

import app.alegon.aws.sct.migrator.model.MigrationProject;
import app.alegon.aws.sct.migrator.persistence.DataExporter;
import app.alegon.aws.sct.migrator.persistence.DataImporter;

@Service
public class MigrationService {

    private DataExporter dataExporter;

    private DataImporter dataImporter;

    public MigrationService(DataExporter dataExporter, DataImporter dataImporter) {
        this.dataExporter = dataExporter;
        this.dataImporter = dataImporter;
    }

    public void migrate(MigrationProject migrationSchema) {
        migrationSchema.migrationMappings().forEach(migrationMapping -> {
            // extract data from source table
            dataExporter.exportTable(migrationMapping.sourceTable());

            // TODO: temporary process extracted data

            // import data into target table
            dataImporter.importTable(migrationMapping.targetTable());
        });
    }
}
