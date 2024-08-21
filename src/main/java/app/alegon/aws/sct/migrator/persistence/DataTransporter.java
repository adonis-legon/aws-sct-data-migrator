package app.alegon.aws.sct.migrator.persistence;

import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterReceiveException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterRemoveException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterSendException;

public abstract class DataTransporter {

    public String getTransportId(MigrationTable sourceMigrationTable) {
        return String.format("%s__%s", sourceMigrationTable.parentSchema().name(), sourceMigrationTable.name());
    }

    public abstract void receiveTableData(String data, MigrationTable sourceMigrationTable)
            throws DataTransporterReceiveException;

    public abstract String sendTableData(MigrationTable sourceMigrationTable)
            throws DataTransporterSendException;

    public abstract void removeTableData(MigrationTable sourceMigrationTable) throws DataTransporterRemoveException;
}
