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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

class MojoHelper {

  private final JUnitPlatformMojo mojo;

  MojoHelper(JUnitPlatformMojo mojo) {
    this.mojo = mojo;
  }

  // should be in maven with something like BeanConfigurator for xml conf
  void autoConfigure(String markerName, Object dto) {
    final PluginParameterExpressionEvaluator evaluator =
        new PluginParameterExpressionEvaluator(mojo.getMavenSession(), mojo.getMojoExecution());
    Stream.of(dto.getClass().getDeclaredFields()) // for now no inheritance support needed
        .filter(it -> !Modifier.isStatic(it.getModifiers()))
        .peek(it -> it.setAccessible(true))
        .forEach(
            field -> {
              final String key = "${junit-platform." + markerName + '.' + field.getName() + "}";
              String value = null;
              try {
                value = (String) evaluator.evaluate(key, String.class);
              } catch (final ExpressionEvaluationException e) {
                mojo.getLog().warn(e.getMessage(), e);
              }
              if (value != null) {
                final Class<?> type = field.getType();
                if (String.class == type) {
                  set(field, dto, value);
                } else if (boolean.class == type) {
                  set(field, dto, Boolean.parseBoolean(value));
                } else if (List.class == type) { // List<String>
                  set(field, dto, asList(value.split(",")));
                } else if (Map.class == type) { // Map<String, String>
                  set(
                      field,
                      dto,
                      Stream.of(value.split(","))
                          .map(it -> it.split("="))
                          .collect(toMap(it -> it[0], it -> it[1])));
                } else {
                  throw new IllegalArgumentException("Unsupported type: " + type);
                }
              }
            });
  }

  private static void set(Field field, Object target, Object value) {
    try {
      field.set(target, value);
    } catch (final IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
