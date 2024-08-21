package app.alegon.aws.sct.migrator.util.resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;

import app.alegon.aws.sct.migrator.util.resource.exception.ResourceException;

public class ApplicationResourceProvider extends ResourceProvider {

    private ResourceLoader resourceLoader;

    private ResourcePatternResolver resourcePatternResolver;

    public ApplicationResourceProvider(ResourceLoader resourceLoader, ResourcePatternResolver resourcePatternResolver) {
        this.resourceLoader = resourceLoader;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @Override
    public String getResourceContent(String resourceName) throws IOException {
        Resource resource = resourceLoader
                .getResource(String.format("classpath:/%s/%s", this.getResourcePath(), resourceName));
        Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        return FileCopyUtils.copyToString(reader);
    }

    @Override
    public String getProjectFile() throws ResourceException {
        Resource[] resourceArray;
        try {
            resourceArray = resourcePatternResolver.getResources(this.getResourcePath() + "/*.sct");
        } catch (IOException e) {
            throw new ResourceException("Error searching resources.", e);
        }

        if (resourceArray.length == 0) {
            throw new ResourceException("No project file found in the specified path: " + this.getResourcePath(), null);
        }

        return resourceArray[0].getFilename();
    }

}
