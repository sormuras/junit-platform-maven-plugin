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

package de.sormuras.junit.platform.maven.plugin;

import static java.util.Collections.emptySet;

import java.net.URI;
import java.util.Set;

/** Selectors passed to the JUnit Platform Launcher to discover tests. */
@SuppressWarnings("WeakerAccess")
public class Selectors {

  Set<String> directories = emptySet();
  Set<String> files = emptySet();

  Set<String> modules = emptySet();
  Set<String> packages = emptySet();
  Set<String> classes = emptySet();
  Set<String> methods = emptySet();

  Set<String> resources = emptySet();

  Set<URI> uris = emptySet();

  boolean isEmpty() {
    return directories.isEmpty()
        && files.isEmpty()
        && modules.isEmpty()
        && packages.isEmpty()
        && classes.isEmpty()
        && methods.isEmpty()
        && resources.isEmpty()
        && uris.isEmpty();
  }
}
