# JUnit Platform Maven Plugin
 
[![jdk8](https://img.shields.io/badge/java-8-lightgray.svg)](http://jdk.java.net/8)
[![jdk21](https://img.shields.io/badge/java-21-blue.svg)](http://jdk.java.net/21)
[![CI](https://github.com/sormuras/junit-platform-maven-plugin/workflows/CI/badge.svg)](https://github.com/sormuras/junit-platform-maven-plugin/actions)
[![stable](https://img.shields.io/badge/api-stable-green.svg)](https://javadoc.io/doc/de.sormuras.junit/junit-platform-maven-plugin)
[![central](https://img.shields.io/maven-central/v/de.sormuras.junit/junit-platform-maven-plugin.svg)](https://search.maven.org/artifact/de.sormuras.junit/junit-platform-maven-plugin)

Maven Plugin launching the JUnit Platform

## Features

* Utilize JUnit Platform's ability to execute multiple `TestEngine`s natively.
* Autoload well-known engine implementations at test runtime: users only have to depend on `junit-jupiter-api`, the Jupiter TestEngine is provided.
* Support in-module and extra-modular testing when writing modularized projects.
* Most [selectors](https://junit.org/junit5/docs/current/api/org/junit/platform/engine/discovery/package-summary.html) the JUnit Platform offers are supported.
* Load test, main, and framework/plugin classes in separation via dedicated `ClassLoader` instances using the [JUnit Platform Isolator](https://github.com/sormuras/junit-platform-isolator) library.

This plugin was presented by [Sander Mak](https://github.com/sandermak) at Devoxx 2018: https://youtu.be/l4Dk7EF-oYc?t=2346

## Prerequisites

Using this plugin requires at least:

* [Apache Maven 3.3.9](https://maven.apache.org)
* [Java 8](http://jdk.java.net/8) to run this plugin
* [Java 21](http://jdk.java.net/21) to build this project

## Simple Usage

The following sections describe the default and minimal usage pattern of this plugin.

### JUnit Jupiter API

Add test compile dependencies into your project's `pom.xml`.
For example, if you want to write tests using the JUnit Jupiter API, you only need the [`junit-jupiter`](https://junit.org/junit5/docs/current/user-guide/#writing-tests) artifact:

```xml
<dependencies>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Configure the `junit-platform-maven-plugin` like this in the `<build><plugins>`-section:

```xml
<plugin>
  <groupId>de.sormuras.junit</groupId>
  <artifactId>junit-platform-maven-plugin</artifactId>
  <version>1.1.7</version>
  <extensions>true</extensions> <!-- Necessary to execute it in 'test' phase. -->
  <configuration>
    <isolation>NONE</isolation> <!-- Version 1.0.0 defaults to ABSOLUTE. -->
  </configuration>
</plugin>
```

This minimal configuration uses the _extensions_ facility to:

- ...inject this plugin's `launch` goal into the `test` phase of Maven's lifecycle.
- ...and also it effectively disables Maven's Surefire plugin by clearing all executions from the `test` phase.

### Pure Maven Plugin Mode

If you want to execute this plugin side-by-side with Surefire you have two options.

Either use the `<extensions>true</extensions>` as described above and also set the following system property to `true`:
`junit.platform.maven.plugin.surefire.keep.executions`.

Or omit the `<extensions>true</extensions>` line (or set it to `false`) and register this plugin's `launch` goal manually to the `test` phase:

```xml
<plugin>
  <groupId>de.sormuras.junit</groupId>
  <artifactId>junit-platform-maven-plugin</artifactId>
  <version>1.1.7</version>
  <extensions>false</extensions> <!-- Neither install this plugin into `test` phase, nor touch Surefire. -->
  <executions>
    <execution>
      <id>Launch JUnit Platform</id>
      <phase>test</phase>
      <goals>
        <goal>launch</goal>
      </goals>
      <configuration>
      ...
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Access SNAPSHOT version via JitPack

Current `master-SNAPSHOT` version is available via [JitPack](https://jitpack.io/#sormuras/junit-platform-maven-plugin):

```xml
<project>
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
</project>
```

## JUnit Platform Configuration

The following sections describe how to pass arguments to the JUnit Platform.
The parameters described below are similar to those used by the [Console Launcher](https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher) on purpose.

### Class Name Patterns

Provide regular expressions to include only classes whose fully qualified names match.
To avoid loading classes unnecessarily, the default pattern only includes class names that begin with `"Test"` or end with `"Test"` or `"Tests"`.

The configuration below extends the default pattern to include also class names that end with `"TestCase"`:

```xml
<configuration>
  <classNamePatterns>
    <pattern>^(Test.*|.+[.$]Test.*|.*Tests?)$</pattern>
    <pattern>.*TestCase</pattern>
  </classNamePatterns>
</configuration>
```

### Tags

Tags or tag expressions to include only tests whose tags match.

https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions

```xml
<configuration>
  <tags>
    <tag>foo</tag>
    <tag>bar</tag>
  </tags>
</configuration>
```

### Additional Custom Configuration Parameters

https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params

```xml
<configuration>
  <parameters>
    <junit.jupiter.execution.parallel.enabled>true</junit.jupiter.execution.parallel.enabled>
    <ninety.nine>99</ninety.nine>
  </parameters>
</configuration>
```

### Selectors

https://junit.org/junit5/docs/current/api/org/junit/platform/engine/discovery/package-summary.html

```xml
<configuration>
  <selectors>
    <classes>
      <class>JupiterTest</class>
      <class>JupiterTests</class>
      <class>TestJupiter</class>
    </classes>
  </selectors>
</configuration>
```

All supported selectors are listed below:

```java
class Selectors {
  Set<String> directories = emptySet();
  Set<String> files = emptySet();

  Set<String> modules = emptySet();
  Set<String> packages = emptySet();
  Set<String> classes = emptySet();
  Set<String> methods = emptySet();

  Set<String> resources = emptySet();

  Set<URI> uris = emptySet();
}  
```

## Plugin Configuration

The following sections describe how to configure the JUnit Platform Maven Plugin.

### Dry Run

Dry-run mode discovers tests but does not execute them.

```xml
<configuration>
  <dryRun>true|false</dryRun>
</configuration>
```

Defaults to `false`.

### Global Timeout

Global timeout duration defaults to 300 seconds.

```xml
<configuration>
  <timeout>300</timeout>
</configuration>
```

### Execution Progress

Duration between output and error log file sizes during execution (JAVA execution mode only). Defaults to 60 seconds.

```xml
<configuration>
  <executionProgress>60</executionProgress>
</configuration>
```

### Isolation Level

`ClassLoader` hierarchy configuration.

```xml
<configuration>
  <isolation>ABSOLUTE|ALMOST|MERGED|NONE</isolation>
</configuration>
```

Defaults to `NONE`.

#### Isolation: ABSOLUTE

Total isolation.

```text
 MAIN
   - target/classes
   - main dependencies...
 TEST
   - target/test-classes
   - test dependencies...
 JUNIT PLATFORM
   - junit-platform-launcher
   - junit-jupiter-engine
   - junit-vintage-engine
   - more runtime-only test engines...
 ISOLATOR
   - junit-platform-isolator
   - junit-platform-isolator-worker
```

#### Isolation: ALMOST

Almost total isolation - main and test classes are put into the same layer.

```text
 MAIN
   - main dependencies...
 TEST
   - target/classes
   - target/test-classes
   - test dependencies...
 JUNIT PLATFORM
   - junit-platform-launcher
   - junit-jupiter-engine
   - junit-vintage-engine
   - more runtime-only test engines...
 ISOLATOR
   - junit-platform-isolator
   - junit-platform-isolator-worker
```

#### Isolation: MERGED

Merge main and test layers.

```text
 MERGED (TEST + MAIN)
   - target/test-classes
   - test dependencies...
   - target/classes
   - main dependencies...
 JUNIT PLATFORM
   - junit-platform-launcher
   - junit-jupiter-engine
   - junit-vintage-engine
   - more runtime-only test engines...
 ISOLATOR
   - junit-platform-isolator
   - junit-platform-isolator-worker
```

#### Isolation: NONE

No isolation, all dependencies are put into a single layer.

```text
 ALL
   - target/classes
   - main dependencies...
   - target/test-classes
   - test dependencies...
   - junit-platform-launcher
   - junit-jupiter-engine
   - junit-vintage-engine
   - more runtime-only test engines...
   - junit-platform-isolator
   - junit-platform-isolator-worker
```

### Executor

The JUnit Platform Maven Plugin supports two modes of execution: DIRECT and JAVA.

```xml
<configuration>
  <executor>DIRECT|JAVA</executor>
</configuration>
```

DIRECT is the default execution mode.

#### Executor: DIRECT

Launch the JUnit Platform Launcher "in-process".
Direct execution doesn't support any special options - it inherits all Java-related settings from Maven's Plugin execution "sandbox".

#### Executor: JAVA

Fork new a JVM calling `java` via Java's Process API and launch the JUnit Platform Console Launcher.

```java
class JavaOptions {
  /**
   * This is the path to the {@code java} executable.
   *
   * <p>When this parameter is not set or empty, the plugin attempts to load a {@code jdk} toolchain
   * and use it to find the {@code java} executable. If no {@code jdk} toolchain is defined in the
   * project, the {@code java} executable is determined by the current {@code java.home} system
   * property, extended to {@code ${java.home}/bin/java[.exe]}.
   */
  String executable = "";

  /** Passed as {@code -Dfile.encoding=${encoding}, defaults to {@code UTF-8}. */
  String encoding = "UTF-8";

  /** Play nice with calling process. */
  boolean inheritIO = false;

  /** Override <strong>all</strong> Java command line options. */
  List<String> overrideJavaOptions = emptyList();

  /** Override <strong>all</strong> JUnit Platform Console Launcher options. */
  List<String> overrideLauncherOptions = emptyList();

  /** Additional Java command line options prepended to auto-generated options. */
  List<String> additionalOptions = emptyList();

  /** Argument for the {@code --add-modules} options: like {@code ALL-MODULE-PATH,ALL-DEFAULT}. */
  String addModulesArgument = "";
}
```

Example

```xml
<configuration>
  <executor>JAVA</executor>
  <javaOptions>
    <inheritIO>true</inheritIO>
    <additionalOptions>
      <additionalOption>--show-version</additionalOption>
      <additionalOption>--show-module-resolution</additionalOption>
    </additionalOptions>
  </javaOptions>
</configuration>
```

## Plugin Configuration Tweaks

Tweak options to fine-tune test execution.

```java
class Tweaks {
  /** Fail test run if no tests are found. */
  boolean failIfNoTests = true;

  /** Enable execution of Java language's {@code assert} statements. */
  boolean defaultAssertionStatus = true;

  /** Use platform or thread context classloader. */
  boolean platformClassLoader = true;

  /** Move any test engine implementations to the launcher classloader. */
  boolean moveTestEnginesToLauncherClassLoader = true;

  /** Fail if worker is not loaded in isolation. */
  boolean workerIsolationRequired = true;

  /** A missing test output directory and no explicit selector configured: skip execution. */
  boolean skipOnMissingTestOutputDirectory = true;

  /** Force ansi to be disabled for java executions. */
  boolean disableAnsi = false;

  /** List of additional raw (local) test path elements. */
  List<String> additionalTestPathElements = emptyList();

  /** List of additional raw (local) launcher path elements. */
  List<String> additionalLauncherPathElements = emptyList();

  /** List of {@code group:artifact} dependencies to exclude from all path sets. */
  List<String> dependencyExcludes = emptyList();

  /** List of {@code group:artifact:version} dependencies to include in test path set. */
  List<String> additionalTestDependencies = emptyList();

  /** List of {@code group:artifact:version} dependencies to include in launcher path set. */
  List<String> additionalLauncherDependencies = emptyList();
}
```

### Error "No tests found."

If the plugin reports "No tests found." it may be due to:

- no tests are declared in `src/test/...` or they are invalid,
- discovery selectors (module, package, class, method, uri, ...) did not select a single container/test,
- discovery filters (engine, class, method, tag expressions, ...) did not match a single container/test,
- or some other environment or system condition that prevented any test to be found.

Possible solutions:
- Create tests below `src/test/...` - it is easy and fun!
- If your `src/test` directory is empty, delete it. The plugin auto-skip test execution if there's no `src/test` directory.
- Tweak the plugin configuration not to fail on no tests found:

```xml
<configuration>
  <tweaks>
    <failIfNoTests>false</failIfNoTests>
  </tweaks>
</configuration>
```

## Modular Testing

https://sormuras.github.io/blog/2018-09-11-testing-in-the-modular-world.html

### Modular Test Mode

A test mode is defined by the relation of one **main** and one **test** module name.

- `C` = `CLASSIC` -> no modules available
- `M` = `MODULAR` -> main `module foo` and test `module bar` OR main lacks module and test `module any`
- `A` = `MODULAR_PATCHED_TEST_COMPILE` -> main `module foo` and test `module foo`
- `B` = `MODULAR_PATCHED_TEST_RUNTIME` -> main `module foo` and test lacks module

```text
                          main plain    main module   main module
                             ---            foo           bar
     test plain  ---          C              B             B
     test module foo          M              A             M
     test module bar          M              M             A
```

Copied from [junit-platform-isolator/.../TestMode.java](https://github.com/sormuras/junit-platform-isolator/blob/master/junit-platform-isolator-base-8/src/main/java/de/sormuras/junit/platform/isolator/TestMode.java)

```java
class TestMode {
  static TestMode of(String main, String test) {
    var mainAbsent = main == null || main.trim().isEmpty();
    var testAbsent = test == null || test.trim().isEmpty();
    if (mainAbsent) {
      if (testAbsent) { // trivial case: no modules declared at all
        return CLASSIC;
      }
      return MODULAR; // only test module is present, no patching involved
    }
    if (testAbsent) { // only main module is present
      return MODULAR_PATCHED_TEST_RUNTIME;
    }
    if (main.equals(test)) { // same module name
      return MODULAR_PATCHED_TEST_COMPILE;
    }
    return MODULAR; // bi-modular testing, no patching involved
  }
}
```

### `module-info.test` support

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
