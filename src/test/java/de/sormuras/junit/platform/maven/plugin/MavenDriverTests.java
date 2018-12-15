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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavenDriverTests {

  @Test
  void pruneDuplicates() {
    Map<String, List<String>> paths = new LinkedHashMap<>();
    paths.put("alpha", list("a", "b", "c"));
    paths.put("beta", list("b", "d", "e"));
    paths.put("gamma", list("c", "d", "e", "f"));

    assertEquals("{alpha=[a, b, c], beta=[b, d, e], gamma=[c, d, e, f]}", paths.toString());
    MavenDriver.pruneDuplicates(paths);
    assertEquals("{alpha=[a, b, c], beta=[d, e], gamma=[f]}", paths.toString());
  }

  private static List<String> list(String... strings) {
    return new ArrayList<>(Arrays.asList(strings));
  }
}
