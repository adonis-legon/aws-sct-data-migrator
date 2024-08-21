package app.alegon.aws.sct.migrator.util.resource;

import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class ResourceProviderFactory {

    private ResourceLoader resourceLoader;

    private ResourcePatternResolver resourcePatternResolver;

    public ResourceProviderFactory(ResourceLoader resourceLoader, ResourcePatternResolver resourcePatternResolver) {
        this.resourceLoader = resourceLoader;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public ResourceProvider getResourceProvider(ResourceProviderType resourceProviderType, String resoucePath) {
        ResourceProvider resourceProvider = switch (resourceProviderType) {
            case FileSystem -> new FileSystemResourceProvider();
            case ApplicationResource -> new ApplicationResourceProvider(resourceLoader, resourcePatternResolver);
            default ->
                throw new IllegalArgumentException("Unsupported resource provider type: " + resourceProviderType);
        };

        resourceProvider.setResourcePath(resoucePath);
        return resourceProvider;
    }
}
