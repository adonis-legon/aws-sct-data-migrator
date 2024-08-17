package app.alegon.aws.sct.migrator.persistence.postgres;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.DataImporter;

@Component
public class PostgresDataImporter implements DataImporter {

    @Override
    public void importTable(MigrationTable table) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'importTable'");
    }

}
