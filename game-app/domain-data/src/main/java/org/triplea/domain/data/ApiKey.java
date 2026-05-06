package org.triplea.domain.data;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An OAuth token, represents an authorization token passed to client from server on successful
 * authentication. The token is passed back to the server on subsequent requests to prove
 * authorization.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiKey {
  public static final int MAX_LENGTH = 36;

  private final String value;

  public static ApiKey newKey() {
    return of(UUID.randomUUID().toString());
  }

  public static ApiKey of(final String value) {
    if (value == null || value.isBlank() || value.contains("\n") || value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid API key passed with length: %d", value == null ? 0 : value.length()));
    }
    return new ApiKey(value);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof ApiKey)) return false;
    return value.equals(((ApiKey) other).value);
  }

  public boolean equals(String other) {
    return value.equals(other);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
