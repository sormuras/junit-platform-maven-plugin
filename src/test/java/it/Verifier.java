/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Integration test verification tool. */
public class Verifier {

  private final Path basedir;
  private final List<String> buildLogLines;
  private boolean ok;

  public Verifier(Path basedir) throws Exception {
    this.basedir = basedir;
    this.buildLogLines = Files.readAllLines(basedir.resolve("build.log"));
    this.ok = true;

    int size = buildLogLines.size();
    System.out.printf("%nVerification tool created: %s%n", basedir);
    out("build.log contains %d lines", size);
    if (buildLogLines.size() > 0) {
      out("%4d: %s", 1, buildLogLines.get(0));
      out("...");
      out("%4d: %s", size, buildLogLines.get(size - 1));
    }
  }

  private void out(String format, Object... args) {
    System.out.println("      | " + String.format(format, args));
  }

  private void err(String format, Object... args) {
    System.err.println("[ERROR] " + String.format(format, args));
  }

  public boolean isOk() {
    return ok;
  }

  public Verifier verifyBadLines() {
    System.out.printf("Verifying build.log doesn't contain bad lines...%n");
    int size = buildLogLines.size();
    for (int i = 0; i < size; i++) {
      String line = buildLogLines.get(i);
      if (line.startsWith("[ERROR]")) {
        ok = false;
        err("Log line %d contains an error marker: %s", i, line);
        continue;
      }
      if (line.startsWith("[WARNING]")) {
        ok = false;
        err("Log line %d contains a warning marker: %s", i, line);
        continue;
      }
      if (line.startsWith("[INFO] --- maven-surefire-plugin:2.12.4:test (default-test)")) {
        ok = false;
        err("Log line %d contains Surefire's default execution marker: %s", i, line);
        //noinspection UnnecessaryContinue
        continue;
      }
    }
    if (ok) {
      out("No marker found in %d build log lines", size);
    }
    return this;
  }

  public Verifier verifyReadable(String... paths) throws Exception {
    System.out.printf("Verifying %d readable file paths...%n", paths.length);
    for (String name : paths) {
      Path path = basedir.resolve(name);
      if (!Files.isReadable(path)) {
        err("Expected file not found: %s", path);
        ok = false;
        continue;
      }
      long size = Files.size(path);
      if (size == 0) {
        err("Expected file %s not to be empty", path);
        ok = false;
        continue;
      }
      out("%s is readable and contains %d bytes", name, size);
    }
    return this;
  }

  public Verifier verifyNotExists(String... paths) {
    System.out.printf("Verifying %d non-existent file paths...%n", paths.length);
    for (String name : paths) {
      Path path = basedir.resolve(name);
      if (Files.exists(path)) {
        err("Expected file %s not to exist", path);
        ok = false;
        continue;
      }
      out("%s doesn't exist", name);
    }
    return this;
  }

  public Verifier verifyLogMatches(String... lines) {
    System.out.printf("Verifying %d expected lines match this build.log...%n", lines.length);
    List<String> expectedLines = Arrays.asList(lines);
    try {
      assertLinesMatch(expectedLines, buildLogLines);
      expectedLines.forEach(line -> out(line));
    } catch (AssertionError e) {
      err("Expected lines don't match this build.log!");
      expectedLines.forEach(line -> err(line));
      ok = false;
    }
    return this;
  }
}
