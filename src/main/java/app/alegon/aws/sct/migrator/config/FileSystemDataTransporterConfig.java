package app.alegon.aws.sct.migrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.persistence.transporter.file-system")
public class FileSystemDataTransporterConfig {
    private String temporalPath;

    public String getTemporalPath() {
        return this.temporalPath;
    }

    public void setTemporalPath(String temporalPath) {
        this.temporalPath = temporalPath;
    }

}
