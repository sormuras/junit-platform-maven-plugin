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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;
import org.apache.maven.plugin.logging.Log;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

class JUnitPlatformCaller implements IntSupplier {

  private final ClassLoader classLoader;
  private final JUnitPlatformLauncher launcher;
  private final Log log;
  private final long timeout;

  public JUnitPlatformCaller(ClassLoader classLoader, Configuration configuration) {
    this.classLoader = classLoader;
    this.launcher = new JUnitPlatformLauncher(configuration);
    this.log = configuration.getLog();
    this.timeout = configuration.getTimeout().toMillis();
  }

  @Override
  public int getAsInt() {
    ClassLoader oldContext = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      return launch();
    } finally {
      Thread.currentThread().setContextClassLoader(oldContext);
    }
  }

  private int launch() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<TestExecutionSummary> future = executor.submit(launcher);
    try {
      return summarize(future.get(timeout, TimeUnit.MILLISECONDS));
    } catch (InterruptedException e) {
      log.error("Launcher execution interrupted", e);
      return -1;
    } catch (ExecutionException e) {
      throw rethrow(e.getCause());
    } catch (TimeoutException e) {
      log.error("Global timeout reached: " + timeout + " millis", e);
      return -2;
    }
  }

  private int summarize(TestExecutionSummary summary) {
    long duration = summary.getTimeFinished() - summary.getTimeStarted();
    boolean success = summary.getTestsFailedCount() == 0 && summary.getContainersFailedCount() == 0;
    if (success) {
      long succeeded = summary.getTestsSucceededCount();
      log.info(String.format("Successfully executed: %d test(s) in %d ms", succeeded, duration));
    } else {
      StringWriter message = new StringWriter();
      PrintWriter writer = new PrintWriter(message);
      summary.printTo(writer);
      summary.printFailuresTo(writer);
      for (String line : message.toString().split("\\R")) {
        log.error(line);
      }
    }
    return success ? 0 : 1;
  }

  private static UndeclaredThrowableException rethrow(Throwable cause) {
    if (cause instanceof RuntimeException) {
      throw (RuntimeException) cause;
    }
    if (cause instanceof Error) {
      throw (Error) cause;
    }
    return new UndeclaredThrowableException(cause);
  }
}
