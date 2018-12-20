# JUnit Platform Maven Plugin
 
[![jdk8](https://img.shields.io/badge/java-8-lightgray.svg)](http://jdk.java.net/8)
[![jdk11](https://img.shields.io/badge/java-11-blue.svg)](http://jdk.java.net/11)
[![travis](https://travis-ci.com/sormuras/junit-platform-maven-plugin.svg?branch=master)](https://travis-ci.com/sormuras/junit-platform-maven-plugin)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](https://javadoc.io/doc/de.sormuras.junit/junit-platform-maven-plugin)
[![central](https://img.shields.io/maven-central/v/de.sormuras.junit/junit-platform-maven-plugin.svg)](https://search.maven.org/artifact/de.sormuras.junit/junit-platform-maven-plugin)

Maven Plugin launching the JUnit Platform

## Goals

* Utilize JUnit Platform's ability to execute multiple `TestEngine`s natively.
* Auto-load well-known engine implementations at test runtime: users only have to depend on `junit-jupiter-api`, the Jupiter TestEngine is provided.
* Support _white-box_ and _black-box_ testing when writing modularized projects.

Idea of this plugin was presented by [Sander Mak](https://github.com/sandermak) at Devoxx 2018: https://youtu.be/l4Dk7EF-oYc?t=2346

## Prequisites

Using this plugin requires at least:

* [Apache Maven 3.3.9](https://maven.apache.org)
* [Java 8](http://jdk.java.net/8)

## Usage with Jupiter

Add test compile dependencies into the `pom.xml`.
For example, if you want to write tests using the Jupiter API, you'll need the [`junit-jupiter-api`](https://junit.org/junit5/docs/current/user-guide/#writing-tests) artifact:

```xml
<dependencies>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.3.1</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Configure the `junit-platform-maven-plugin` like this in the `<build><plugins>`-section:

```xml
<plugin>
  <groupId>de.sormuras.junit</groupId>
  <artifactId>junit-platform-maven-plugin</artifactId>
  <version>1.0.0-M2</version>  
  <extensions>true</extensions> <!-- Necessary to execute it in 'test' phase. -->
</plugin>
```

### JitPack

Current `master-SNAPSHOT` version is available via [JitPack](https://jitpack.io/#sormuras/junit-platform-maven-plugin):

```xml
<pluginRepositories>
    <pluginRepository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </pluginRepository>
</pluginRepositories>

<dependency>
    <groupId>com.github.sormuras</groupId>
    <artifactId>junit-platform-maven-plugin</artifactId>
    <version>master-SNAPSHOT</version>
</dependency>
```

## `module-info.test` support

Needs `<executor>JAVA</executor>` mode!

This plugin also integrates additional compiler flags specified in a `module-info.test` file.
For example, if your tests need to access types from a module shipping with the JDK (here: `java.scripting`).
Note that each non-comment line represents a single argument that is passed to the compiler as an option.

```text
// Make module visible.
--add-modules
  java.scripting

// Same as "requires java.scripting" in a regular module descriptor.
--add-reads
  greeter.provider=java.scripting
```

See `src/it/modular-world-2-main-module-test-plain` for details.

## Contribution Policy

Contributions via GitHub pull requests are gladly accepted from their original author.
Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license.
Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License

This code is open source software licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.html).
