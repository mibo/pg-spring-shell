# Spring Shell Playground

## Start with profile

  - Option 1: Java System Properties (VM Arguments): `java -jar -Dspring.profiles.active=local application.jar`
  - Option 2: Program arguments: `java -jar application.jar --spring.profiles.active=prod --spring.config.location=./config`
  - Option 2b: Program arguments for GitHub sample: `java -jar application.jar --spring.profiles.active=sample`

### Secure connect

The _Secure Connect_ is in the `secure-connect` directory.
After build with `gradle assemble` the build jar (`secure-connect-shell.jar`) can be run.
Only parameter is the config file to be used. Either as absolut path or as filename which can resolved by the classloader. 

  - As sample start with `java -jar build/libs/secure-connect-shell.jar --config=/path/to/config.yaml`

### Reference Documentation
For further reference, please consider the following sections:

  * [Official Gradle documentation](https://docs.gradle.org)
  * [Kotlin documentation](https://kotlinlang.org/docs/tutorials/getting-started.html)
  * [Spring Shell Reference Documentation](https://docs.spring.io/spring-shell/docs/current/reference/htmlsingle/)
  * [CLI with Spring Shell by Baeldung](https://www.baeldung.com/spring-shell-cli)
  * [Pass arguments to CLI](https://codeboje.de/spring-boot-commandline-app-args/)

### Additional Links
These additional references should also help you:

  * Gradle
      * [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
      * [Structuring and Building a Software Component with Gradle](https://docs.gradle.org/current/userguide/multi_project_builds.html)
      * [Building Java Applications with libraries Sample](https://docs.gradle.org/current/samples/sample_building_java_applications_multi_project.html)
