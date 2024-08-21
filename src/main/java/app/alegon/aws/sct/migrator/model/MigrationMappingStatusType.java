package app.alegon.aws.sct.migrator.model;

import app.alegon.aws.sct.migrator.util.resource.ResourceProviderType;

public enum MigrationMappingStatusType {
    Started,
    InProgress,
    Successful,
    Failed;

    public static ResourceProviderType fromValue(String value) {
        for (ResourceProviderType type : ResourceProviderType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid MigrationMappingStatusType value: " + value);
    }

    public String getValue() {
        return this.name().toLowerCase();
    }
}
