# Data migration tool based on an AWS Schema Conversion Tool Project

## Requirements

- Java 21 or later
- An existing AWS Schema Conversion Tool directory with all files for a schema migration project

## Steps to create project directory

IMPORTANT: the user that connects to the source and target database, must have privileges to access data dictionary and other permissions defined on the tool's documentation

1. Click *New project*
2. Click *Add source* to connect to a source database
3. Click *Add target* to connect to a target database
4. Select source Database/Schema and select target Database and Click *Create Mapping*
5. Go to *Main View*
6. In the source, select the Database and right click to *Convert Schema*
7. After the schema conversion is applied to the target database's schema, there might be objects that were not possible to directly migrate (marked as red on the source databse schema), in that case:
    1. Go to *Assesment Report View*
    2. Check the Summary for the missing objects not migrated
    3. Go to Action Items tab to check those objects and perform manual conversion, and then run the *Convert Schema* step again
8. After all source schema objects are converted, then go to target schema and right click for *Apply to database*
9. After all schema objects are applied to target database, the resulting AWS SCT project directory files can be now passed as input to this data migration tool

Reference video: https://www.youtube.com/watch?v=ibtNkChGFkw&t=85s

## Supported Migrations

- Oracle to PostgreSQL
- MSSQL to PostgreSQL
- Oracle to MySQL
- MSSQL to MySQL

### Special considerations

- In the case of data migration into MySQL, there should be a configuration set to ON on the DbEngine which is: local_infile=ON, to be able to support remote file data import into MySQL

## Build

```console
mvn clean package -DskipTests
```

## Run (Console mode)

```console
java -jar aws-sct-data-migrator-{version}.jar --mode=console --project-path=/aws/sct/project/directory
```

## Run (Service mode)

```console
export MIGRATION_SERVICE_SCHEDULE={CRON expression for migration task execution}
export MIGRATION_SERVICE_TIMEZONE={Time Zone}
export MIGRATION_SERVICE_PROJECT_PATH={Project Path}
export MIGRATION_SERVICE_SOURCE_PASSWORD={Source Database Password for the user in the AWS SCT Project}
export MIGRATION_SERVICE_TARGET_PASSWORD={Target Database Password for the user in the AWS SCT Project}
java -jar aws-sct-data-migrator-{version}.jar --mode=service
```

## CHANGELOG

1. v0.0.1: MVP supporting basic features
    1. Load AWS Schema Conversion Tool's Project files to detect source and target schemas, datasources, table structures, primary keys and table relations
    2. Determine data migration plan based on table dependencies
    3. Support for Data Extractors:
        1. Oracle Database
        2. Microsoft SQL Server (MSSQL) 
    4. Support for Data Loaders:
        1. PosgreSQL
        2. MySQL
    5. Support for console based execution to perform manual data migrations
2. v0.0.2: Support for service based execution for "low volume" data migration scenarios to keep running frequently as a service
    1. Load data into target datasource avoiding primary key violation due to duplicates
