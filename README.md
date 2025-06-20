[![Hibernate team logo](http://static.jboss.org/hibernate/images/hibernate_logo_whitebkg_200px.png)](https://hibernate.org/reactive)

[![Main branch build status](https://img.shields.io/github/actions/workflow/status/hibernate/hibernate-reactive/build.yml?label=Hibernate%20Reactive%20CI&style=for-the-badge)](https://github.com/hibernate/hibernate-reactive/actions?query=workflow%3A%22Hibernate+Reactive+CI%22)
[![Apache 2.0 license](https://img.shields.io/badge/License-APACHE%202.0-green.svg?logo=APACHE&style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![Latest version on Maven Central](https://img.shields.io/maven-central/v/org.hibernate.reactive/hibernate-reactive-core.svg?label=Maven%20Central&logo=apache-maven&style=for-the-badge)](https://search.maven.org/search?q=g:org.hibernate.reactive)
[![Developers stream on Zulip](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg?logo=zulip&style=for-the-badge)](https://hibernate.zulipchat.com/#narrow/stream/205413-hibernate-reactive-dev)
[![Hibernate Reactive documentation](https://img.shields.io/badge/Hibernate-Documentation-orange.svg?logo=Hibernate&style=for-the-badge)](https://hibernate.org/reactive/documentation/)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/hibernate/reactive/hibernate-reactive/badge.json&style=for-the-badge)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/hibernate/reactive/hibernate-reactive/README.md)

# Hibernate Reactive

A reactive API for [Hibernate ORM][], supporting non-blocking database
drivers and a reactive style of interaction with the database.

Hibernate Reactive may be used in any plain Java program, but is 
especially targeted toward usage in reactive environments like 
[Quarkus][] and [Vert.x][].

Currently [PostgreSQL][], [MySQL][], [MariaDB][], [Db2][], 
[CockroachDB][], [MS SQL Server][MSSQL] and [Oracle][] are supported.

Learn more at <http://hibernate.org/reactive>.

[Hibernate ORM]: https://hibernate.org/orm/
[Quarkus]: https://quarkus.io
[Quarkus quickstarts]: https://github.com/quarkusio/quarkus-quickstarts
[Vert.x]: https://vertx.io

## Compatibility

Hibernate Reactive has been tested with:

- Java 17, 21, 24
- PostgreSQL 16
- MySQL 9
- MariaDB 11
- Db2 12
- CockroachDB v24
- MS SQL Server 2022
- Oracle 23
- [Hibernate ORM][] 7.0.2.Final
- [Vert.x Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/) 5.0.0
- [Vert.x Reactive MySQL Client](https://vertx.io/docs/vertx-mysql-client/java/) 5.0.0
- [Vert.x Reactive Db2 Client](https://vertx.io/docs/vertx-db2-client/java/) 5.0.0
- [Vert.x Reactive MS SQL Server Client](https://vertx.io/docs/vertx-mssql-client/java/) 5.0.0
- [Vert.x Reactive Oracle Client](https://vertx.io/docs/vertx-oracle-client/java/) 5.0.0
- [Quarkus][Quarkus] via the Hibernate Reactive extension

[PostgreSQL]: https://www.postgresql.org
[MySQL]: https://www.mysql.com
[MariaDB]: https://mariadb.com
[DB2]: https://www.ibm.com/analytics/db2
[CockroachDB]: https://www.cockroachlabs.com/
[MSSQL]: https://www.microsoft.com/en-gb/sql-server
[Oracle]: https://www.oracle.com/database/

## Documentation

The [Introduction to Hibernate Reactive][introduction] covers 
everything you need to know to get started, including:

- [setting up a project][build] that uses Hibernate Reactive and the 
  Vert.x reactive SQL client for your database,
- [configuring][config] Hibernate Reactive to access your database,
- writing Java code to [define the entities][model] of your data model, 
- writing reactive data access code [using a reactive session][session], 
  and
- [tuning the performance][performance] of your program.

We recommend you start there!

The [Vert.x and Hibernate Reactive How-to][vertx-hr] explains how to use
Hibernate Reactive in Vert.x.

The [Hibernate Reactive with Panache Guide][reactive-panache] introduces
Panache Reactive, an active record-style API based on Hibernate Reactive.

[introduction]: https://github.com/hibernate/hibernate-reactive/blob/main/documentation/src/main/asciidoc/reference/introduction.adoc

[build]: https://github.com/hibernate/hibernate-reactive/blob/main/documentation/src/main/asciidoc/reference/introduction.adoc#including-hibernate-reactive-in-your-project-build
[config]: https://github.com/hibernate/hibernate-reactive/blob/main/documentation/src/main/asciidoc/reference/introduction.adoc#basic-configuration
[model]: https://github.com/hibernate/hibernate-reactive/blob/main/documentation/src/main/asciidoc/reference/introduction.adoc#mapping-entity-classes
[session]: https://github.com/hibernate/hibernate-reactive/blob/main/documentation/src/main/asciidoc/reference/introduction.adoc#using-the-reactive-session
[performance]: https://github.com/hibernate/hibernate-reactive/blob/main/documentation/src/main/asciidoc/reference/introduction.adoc#tuning-and-performance

[vertx-hr]: https://how-to.vertx.io/hibernate-reactive-howto/
[reactive-panache]: https://quarkus.io/guides/hibernate-reactive-panache

## Examples

The directory [`examples`][examples] contains several small projects showing
different features of Hibernate Reactive:

  - [CRUD operations using the session](https://github.com/hibernate/hibernate-reactive/tree/main/examples/session-example)
  - [Native queries and stateless session](https://github.com/hibernate/hibernate-reactive/tree/main/examples/native-sql-example)

[examples]: https://github.com/hibernate/hibernate-reactive/tree/main/examples

## Quarkus quickstarts

A collection of [quickstarts][Quarkus quickstarts] for Quarkus is available on GitHub:

  - [Hibernate Reactive with RESTEasy Reactive](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-reactive-quickstart)
  - [Hibernate Reactive with Panache](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-reactive-panache-quickstart)
  - [Hibernate Reactive with Vert.x Web Routes](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-reactive-routes-quickstart)

Or you can [generate a new Quarkus project](https://code.quarkus.io/?g=org.acme&a=code-with-quarkus&v=1.0.0-SNAPSHOT&b=MAVEN&s=r1s&cn=code.quarkus.io)
that uses the Hibernate Reactive extension and start coding right away.

## Examples using JBang

With [JBang](https://www.jbang.dev/) you can run one of the examples available in the catalog
without having to clone the repository or setup the project in the IDE.
Once you have downloaded JBang, the list of examples is available via: 
```
jbang alias list hibernate/hibernate-reactive
```

If you want to run one of the example (in this case the one called `example`), you can do it with:
```
jbang example@hibernate/hibernate-reactive
```

or you can [open it in your editor](https://github.com/jbangdev/jbang#editing) (IntelliJ IDEA in this case) with:
```
jbang edit --open=idea testcase@hibernate/hibernate-reactive
```

You can also generate and run a db-specific test. See available templates using: `jbang template list`

```
cockroachdb-reproducer = Template for a test with CockroachDB using Junit 4, Vert.x Unit and Testcontainers
db2-reproducer = Template for a test with Db2 using Junit 4, Vert.x Unit and Testcontainers
mariadb-reproducer = Template for a test with MariaDB using Junit 4, Vert.x Unit and Testcontainers
mysql-reproducer = Template for a test with MySQL using Junit 4, Vert.x Unit and Testcontainers
pg-reproducer = Template for a test with PostgreSQL using Junit 4, Vert.x Unit and Testcontainers
```

Example for PostgreSQL:
  - Generate java test from template: `jbang init --template=pg-reproducer pgTest.java`
  - Run the test: `jbang pgTest.java` 

## Gradle build

The project is built with Gradle, but you do _not_ need to have Gradle
installed on your machine.

### Building

To compile this project, navigate to the `hibernate-reactive` directory, 
and type:

    ./gradlew compileJava

To publish Hibernate Reactive to your local Maven repository, run:

    ./gradlew publishToMavenLocal

### Building documentation

To build the API and Reference documentation type:

    ./gradlew assembleDocumentation

You'll find the generated documentation in the subdirectory
`release/build/documentation`.

    open release/build/documentation/reference/html_single/index.html
    open release/build/documentation/javadocs/index.html

### Running tests

To run the tests, you'll need to decide which RDBMS you want to test 
with, and then get an instance of the test database running on your 
machine.

By default, the tests will be run against PostgreSQL. To test against 
a different database, you must explicitly specify it using the property
`-Pdb`, as shown in the table below.

| Database   | Command                      |
|------------|------------------------------|
| PostgreSQL | `./gradlew test -Pdb=pg`     |
| MySQL      | `./gradlew test -Pdb=mysql`  |
| MariaDB    | `./gradlew test -Pdb=maria`  |
| DB2        | `./gradlew test -Pdb=db2`    |
| SQL Server | `./gradlew test -Pdb=mssql`  |
| Oracle     | `./gradlew test -Pdb=oracle` |

It's even possible to run all tests or certain selected tests on
all available databases:

    ./gradlew testAll -PincludeTests=DefaultPortTest

The property `includeTests` specifies the name of the test to run
and may contain the wildcard `*`. This property is optional, but
very useful, since running all tests on all databases might take 
a lot of time.

To enable logging of the standard output streams, add the property 
`-PshowStandardOutput`.

There are three ways to start the test database.
    
#### If you have Docker installed

If you have Docker installed, running the tests is really easy. You
don't need to create the test databases manually. Just type:

    ./gradlew test -Pdocker

The above command will start an instance of PostgreSQL in a Docker
container. You may specify a different database using one of the
commands show in the table below.

| Database   | Command                               |
|------------|---------------------------------------|
| PostgreSQL | `./gradlew test -Pdocker -Pdb=pg`     |
| MySQL      | `./gradlew test -Pdocker -Pdb=mysql`  |
| MariaDB    | `./gradlew test -Pdocker -Pdb=maria`  |
| DB2        | `./gradlew test -Pdocker -Pdb=db2`    |
| SQL Server | `./gradlew test -Pdocker -Pdb=mssql`  |
| Oracle     | `./gradlew test -Pdocker -Pdb=oracle` |

The tests will run faster if you reuse the same containers across 
multiple test runs. To do this, edit the testcontainers configuration 
file `.testcontainers.properties` in your home directory, adding the 
line `testcontainers.reuse.enable=true`. (Just create the file if it 
doesn't already exist.)

#### If you already have PostgreSQL installed

If you already have PostgreSQL installed on your machine, you'll just 
need to create the test database. From the command line, type the 
following commands:

    psql
    create database hreact;
    create user hreact with password 'hreact';
    grant all privileges on database hreact to hreact;
    alter user hreact createdb;

Then run `./gradlew test` from the `hibernate-reactive` directory.

#### If you already have MySQL installed

If you have MySQL installed, you can create the test database using 
the following commands:

    mysql -uroot
    create database hreact;
    create user hreact identified by 'hreact';
    grant all on hreact.* to hreact;

Then run `./gradlew test -Pdb=mysql` from the `hibernate-reactive` 
directory.

#### If you have Podman

If you have [Podman][podman] installed, you can start the test
database by following the instructions in [podman.md](podman.md).

[podman]: https://podman.io

## Limitations

We're working hard to support the full feature set of Hibernate ORM. 
At present several minor limitations remain.

- The annotation `@org.hibernate.annotations.Source` for 
  database-generated `@Version` properties is not yet supported.
- The annotation `@org.hibernate.annotations.CollectionId` is not yet 
  supported.
- With Db2:
  * [Automatic schema](http://hibernate.org/reactive/documentation/1.1/reference/html_single/#_automatic_schema_export) update and validation is not supported.
  * `@Lob` annotation is not supported - See [this issue on the vertx-db2-client](https://github.com/eclipse-vertx/vertx-sql-client/issues/496)
