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

/** Execution mode. */
public enum Executor {

  /** Direct "in-process" execution. */
  DIRECT(false, true),

  /** Fork new JVM calling {@code java} via Process API. */
  JAVA(true, false);

  private final boolean injectConsole;

  private final boolean injectWorker;

  Executor(boolean injectConsole, boolean injectWorker) {
    this.injectConsole = injectConsole;
    this.injectWorker = injectWorker;
  }

  /** @return {@code true} if junit-platform-console is needed at test runtime. */
  public boolean isInjectConsole() {
    return injectConsole;
  }

  /** @return {@code true} if isolator-worker is needed at test runtime. */
  public boolean isInjectWorker() {
    return injectWorker;
  }
}
