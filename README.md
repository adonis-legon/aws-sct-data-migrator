# Data migration tool based on an AWS Schema Conversion Tool Project

## Requirements

- Java 21 or later

## Supported Migrations

- Oracle to PostgreSQL

## Build

```console
mvn clean package -DskipTests
```

## Run

```console
java -jar aws-sct-data-migrator-{version}.jar --project-path /aws/sct/project/directory
```