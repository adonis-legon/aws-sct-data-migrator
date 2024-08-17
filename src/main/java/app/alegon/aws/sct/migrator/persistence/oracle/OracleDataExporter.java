package app.alegon.aws.sct.migrator.persistence.oracle;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.DataExporter;

@Component
public class OracleDataExporter implements DataExporter {

    @Override
    public void exportTable(MigrationTable table) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exportTable'");
    }

}
