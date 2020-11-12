package de.sormuras.junit.platform.maven.plugin;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.platform.launcher.Launcher;

/**
 * {@code LauncherOptions} defines the configuration API for creating {@link Launcher} instances.
 */
public class LauncherOptions {

  /**
   * Determine if test engines should be discovered at runtime using the {@link
   * java.util.ServiceLoader ServiceLoader} mechanism and automatically registered.
   */
  boolean testEngineAutoRegistration = true;

  /**
   * Determine if test execution listeners should be discovered at runtime using the {@link
   * java.util.ServiceLoader ServiceLoader} mechanism and automatically registered.
   */
  boolean testExecutionListenerAutoRegistration = true;

  /** Collection of additional test engines that should be added to the {@link Launcher}. */
  Collection<String> additionalTestEngines = new ArrayList<>();
}
