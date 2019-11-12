## Gradle Git Properties
Because all microservices use Spring Actuator, the plugin [Gradle Git Properties plugin](https://github.com/n0mer/gradle-git-properties) provides extra information in the actuator **_info_** endpoint automatically

## Documents

This application uses [Spring Auto Restdocs](https://github.com/ScaCap/spring-auto-restdocs) and can be accessed at **{host}:{port}/docs** ex. `localhost:8080/docs`.

To see updated documentation the following command must be run (IDE does not generate new documentation):
```shell
./gradlew clean build asciidoctor bootRun
```

## Using SonarQube
Code quality is checked using [SonarScanner for Gradle](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/) like so:

```shell
./gradlew clean build sonarqube
```

To successfully run the sonarqube task, you will need the following properties in **_~/.gradle/gradle.properties_**:
- **systemProp.sonar.host.url**

## Publishing the JAR to Nexus
Publishing to the Nexus server uses the [Maven Publish plugin](https://docs.gradle.org/current/userguide/publishing_maven.html) like so:

```shell
./gradlew clean build publish
```

To successfully publish to the Nexus server, you will need the following properties in **_~/.gradle/gradle.properties_**:
- **nexus.host** *_(This should be the host name of the machine hosting nexus)_*
- **nexus.port.jar** *_(This should be the port for the maven repositories on the machine hosting nexus)_*
- **nexus.user** *_(Self-explanatory dummy)_*
- **nexus.password** *_(Self-explanatory dummy)_*

## Publishing the container to Nexus
Publishing to the Nexus server uses the [Jib Gradle plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin) like so:

```shell
./gradlew clean build jib
```

To successfully publish to the Nexus server, you will need the following properties in **_~/.gradle/gradle.properties_**:
- **nexus.host** *_(This should be the host name of the machine hosting nexus)_*
- **nexus.port.image** *_(This should be the port for the docker repositories on the machine hosting nexus)_*
- **nexus.user** *_(Self-explanatory dummy)_*
- **nexus.password** *_(Self-explanatory dummy)_*

## Releasing the application
Releasing uses the [Gradle Release plugin](https://github.com/researchgate/gradle-release) like so:

```shell
./gradlew clean build release
```

To successfully complete a release, the **_-SNAPSHOT_** will need to be removed from all dependencies and you will need the following property in **_~/.gradle/gradle.properties_**:
- **release.git.remote** *_(Ex. origin, upstream, etc.)_*