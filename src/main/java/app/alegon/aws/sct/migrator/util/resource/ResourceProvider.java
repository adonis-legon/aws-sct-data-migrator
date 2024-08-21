package app.alegon.aws.sct.migrator.util.resource;

import java.io.IOException;

import app.alegon.aws.sct.migrator.util.resource.exception.ResourceException;

public abstract class ResourceProvider {

    private String resourcePath;

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    protected String getResourcePath() {
        return resourcePath;
    }

    public abstract String getResourceContent(String resourceName) throws IOException;

    public abstract String getProjectFile() throws ResourceException;
}
