/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.sormuras.junit.platform.maven.plugin;

import static java.util.stream.Collectors.joining;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Optional;

class Modules {

  private final Path mainPath;
  private final Path testPath;

  Modules(Path mainPath, Path testPath) {
    this.mainPath = mainPath;
    this.testPath = testPath;
  }

  Path getMainPath() {
    return mainPath;
  }

  Path getTestPath() {
    return testPath;
  }

  Optional<ModuleReference> getMainModuleReference() {
    return Optional.ofNullable(getSingleModuleReferenceOrNull(getMainPath()));
  }

  Optional<ModuleReference> getTestModuleReference() {
    return Optional.ofNullable(getSingleModuleReferenceOrNull(getTestPath()));
  }

  static ModuleReference getSingleModuleReferenceOrNull(Path path) {
    var all = ModuleFinder.of(path).findAll();
    var firstOpt = all.stream().findFirst();
    switch (all.size()) {
      case 0:
        return null;
      case 1:
        return firstOpt.get();
      default:
        throw new IllegalArgumentException(
            "expected exact one module in "
                + path
                + " but found: "
                + all.stream().map(ModuleReference::toString).collect(joining(", ", "<", ">")));
    }
  }
}
