package de.sormuras.junit.platform.maven.plugin;

import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;

public class JavaOptions {

  @Parameter private List<String> additionalOptions = List.of();

  @Parameter private String addModules;

  @Parameter private List<String> addOpens;

  @Parameter private List<String> addReads;

  List<String> getAdditionalOptions() {
    return additionalOptions;
  }

  String getAddModules() {
    return addModules;
  }

  List<String> getAddOpens() {
    return addOpens;
  }

  List<String> getAddReads() {
    return addReads;
  }
}
