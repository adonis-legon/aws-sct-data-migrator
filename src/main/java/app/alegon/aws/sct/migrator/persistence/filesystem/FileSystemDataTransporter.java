package app.alegon.aws.sct.migrator.persistence.filesystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import app.alegon.aws.sct.migrator.config.FileSystemDataTransporterConfig;
import app.alegon.aws.sct.migrator.model.MigrationTable;
import app.alegon.aws.sct.migrator.persistence.DataTransporter;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterReceiveException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterRemoveException;
import app.alegon.aws.sct.migrator.persistence.exception.DataTransporterSendException;

@Component("FILESYSTEM")
public class FileSystemDataTransporter extends DataTransporter {

    private FileSystemDataTransporterConfig fileSystemDataTransporterConfig;

    public FileSystemDataTransporter(FileSystemDataTransporterConfig fileSystemDataTransporterConfig) {
        this.fileSystemDataTransporterConfig = fileSystemDataTransporterConfig;
    }

    @Override
    public void receiveTableData(String tableData, MigrationTable sourceMigrationTable)
            throws DataTransporterReceiveException {
        File directory = new File(fileSystemDataTransporterConfig.getTemporalPath());
        if (!directory.exists() && !directory.mkdirs()) {
            throw new DataTransporterReceiveException(
                    "Failed to create directory: " + fileSystemDataTransporterConfig.getTemporalPath(), null);
        }

        String transportId = getTransportId(sourceMigrationTable);
        Path outputFile = Paths.get(fileSystemDataTransporterConfig.getTemporalPath(), transportId);

        try {
            Files.writeString(outputFile, tableData);
        } catch (Exception e) {
            throw new DataTransporterReceiveException("Error in recieve step of the transport process.", e);
        }

    }

    @Override
    public String sendTableData(MigrationTable sourceMigrationTable)
            throws DataTransporterSendException {
        return Paths.get(fileSystemDataTransporterConfig.getTemporalPath(), getTransportId(sourceMigrationTable))
                .toString();
    }

    @Override
    public void removeTableData(MigrationTable sourceMigrationTable) throws DataTransporterRemoveException {
        Path outputFile = Paths.get(fileSystemDataTransporterConfig.getTemporalPath(),
                getTransportId(sourceMigrationTable));
        try {
            Files.deleteIfExists(outputFile);
        } catch (Exception e) {
            throw new DataTransporterRemoveException("Error in remove step of the transport process.", e);
        }
    }

}
