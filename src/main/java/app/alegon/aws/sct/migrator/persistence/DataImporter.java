package app.alegon.aws.sct.migrator.persistence;

import app.alegon.aws.sct.migrator.model.MigrationTable;

public interface DataImporter {
    public void importTable(MigrationTable table);
}
