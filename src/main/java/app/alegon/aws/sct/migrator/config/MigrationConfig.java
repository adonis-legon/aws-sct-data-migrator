package app.alegon.aws.sct.migrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.migration")
public class MigrationConfig {
    private String csvSeparator;

    public String getCsvSeparator() {
        return this.csvSeparator;
    }

    public void setCsvSeparator(String csvSeparator) {
        this.csvSeparator = csvSeparator;
    }

}
