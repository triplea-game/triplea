package org.triplea.server;

import static java.util.Arrays.stream;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Enumerated values of OS environment variable names that will be used to fetch values from the OS.
 */
public enum EnvironmentVariable {
  ERROR_REPORTING_GITHUB_ORG,

  ERROR_REPORTING_GITHUB_REPO,

  GITHUB_API_AUTH_TOKEN;

  static {
    final Collection<String> missingVariables =
        stream(EnvironmentVariable.values())
            .map(EnvironmentVariable::name)
            .filter(name -> System.getenv(name) == null)
            .collect(Collectors.toList());

    if (!missingVariables.isEmpty()) {
      throw new MissingEnvironmentVariableException(missingVariables);
    }
  }

  private static class MissingEnvironmentVariableException extends IllegalStateException {
    private static final long serialVersionUID = -4818938983375170980L;

    MissingEnvironmentVariableException(final Collection<String> missingEnvironmentVariables) {
      super("Environment variables need to be set: " + missingEnvironmentVariables);
    }
  }

  public String getValue() {
    return System.getenv(name());
  }
}
