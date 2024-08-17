package app.alegon.aws.sct.migrator.persistence;

import app.alegon.aws.sct.migrator.model.MigrationTable;

public interface DataExporter {
    public void exportTable(MigrationTable table);
}
