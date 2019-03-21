package org.triplea.lobby.server;

import java.util.Optional;

import com.google.common.base.Preconditions;

import lombok.AllArgsConstructor;

/** Class that represent OS environment variable keys with default values. */
@AllArgsConstructor
public enum EnvironmentVariable {
  PORT("3304"),

  POSTGRES_USER("postgres"),

  POSTGRES_PASSWORD("postgres"),

  POSTGRES_HOST("localhost"),

  POSTGRES_PORT("5432"),

  POSTGRES_DATABASE("lobby"),

  ERROR_REPORTING_GITHUB_ORG("triplea-game"),

  ERROR_REPORTING_GITHUB_REPO("triplea"),

  GITHUB_API_AUTH_TOKEN(""),

  PROD("false");

  private final String defaultValue;

  public String getValue() {
    return Optional.ofNullable(System.getenv(name())).orElse(defaultValue);
  }

  /**
   * When in production, call this method to verify a full configuration is in place.
   */
  public static void verifyProdConfiguration() {
    Preconditions.checkState(
        !GITHUB_API_AUTH_TOKEN.getValue().isEmpty(),
        "Environment variable GITHUB_API_AUTH_TOKEN needs to be set");
  }
}
