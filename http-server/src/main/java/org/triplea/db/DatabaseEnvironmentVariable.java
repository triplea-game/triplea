package org.triplea.db;

import java.util.Optional;
import lombok.AllArgsConstructor;

/** Class that represent OS environment variable keys with default values. */
@AllArgsConstructor
public enum DatabaseEnvironmentVariable {
  POSTGRES_USER("lobby_user"),

  POSTGRES_PASSWORD("postgres"),

  POSTGRES_HOST("localhost"),

  POSTGRES_PORT("5432"),

  POSTGRES_DATABASE("lobby_db");

  private final String defaultValue;

  public String getValue() {
    return Optional.ofNullable(System.getenv(name())).orElse(defaultValue);
  }
}
