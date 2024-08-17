package app.alegon.aws.sct.migrator.model;

import java.util.List;

public record MigrationTable(String name, List<MigrationTableColumn> columns, List<String> tableNameDependencies,
        MigrationSchema parentSchema) {
    public int getDependencyCount() {
        int count = tableNameDependencies.size();
        if (tableNameDependencies.stream().anyMatch(tableName -> name.equalsIgnoreCase(tableName))) {
            count--;
        }

        return count >= 0 ? count : 0;
    }
}