package app.alegon.aws.sct.migrator.model;

public enum DatabaseEngine {
    ORACLE("ORACLE"),
    POSTGRESQL("POSTGRESQL");

    private final String engineName;

    DatabaseEngine(String engineName) {
        this.engineName = engineName;
    }

    public String getEngineName() {
        return engineName;
    }

    public static DatabaseEngine fromString(String engineName) {
        for (DatabaseEngine engine : DatabaseEngine.values()) {
            if (engine.getEngineName().equalsIgnoreCase(engineName)) {
                return engine;
            }
        }
        throw new IllegalArgumentException("Invalid database engine name: " + engineName);
    }
}
