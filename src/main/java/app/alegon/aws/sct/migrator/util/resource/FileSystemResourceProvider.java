package app.alegon.aws.sct.migrator.util.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import app.alegon.aws.sct.migrator.util.resource.exception.ResourceException;

public class FileSystemResourceProvider extends ResourceProvider {

    @Override
    public String getResourceContent(String resourceName) throws IOException {
        return Files.readString(Paths.get(getResourcePath(), resourceName));
    }

    @Override
    public String getProjectFile() throws ResourceException {
        Path firstSctFile;
        try {
            firstSctFile = Files.walk(Paths.get(getResourcePath()))
                    .filter(path -> path.toString().endsWith(".sct"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new ResourceException("Error searching for project file.", e);
        }

        if (firstSctFile == null) {
            throw new ResourceException("No .sct file found in the given path.", null);
        }

        return firstSctFile.getFileName().toString();
    }

}
