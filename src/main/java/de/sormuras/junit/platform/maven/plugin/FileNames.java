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

import java.util.StringJoiner;
import org.apache.maven.plugins.annotations.Parameter;

public class FileNames {

  @Parameter private String consoleLauncherCmdLog = "console-launcher.cmd.log";
  @Parameter private String consoleLauncherErrLog = "console-launcher.err.log";
  @Parameter private String consoleLauncherOutLog = "console-launcher.out.log";

  @Parameter private String moduleInfoTest = "module-info.test";

  String getConsoleLauncherCmdLog() {
    return consoleLauncherCmdLog;
  }

  String getConsoleLauncherErrLog() {
    return consoleLauncherErrLog;
  }

  String getConsoleLauncherOutLog() {
    return consoleLauncherOutLog;
  }

  String getModuleInfoTest() {
    return moduleInfoTest;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", FileNames.class.getSimpleName() + "[", "]")
        .add("consoleLauncherCmdLog='" + consoleLauncherCmdLog + "'")
        .add("consoleLauncherErrLog='" + consoleLauncherErrLog + "'")
        .add("consoleLauncherOutLog='" + consoleLauncherOutLog + "'")
        .add("moduleInfoTest='" + moduleInfoTest + "'")
        .toString();
  }
}
