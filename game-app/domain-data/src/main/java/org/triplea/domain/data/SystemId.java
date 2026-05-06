package org.triplea.domain.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Identifier generated on a users system to uniquely identify that player in addition to their IP
 * address.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SystemId {
  private final String value;

  public static SystemId of(final String systemId) {
    if (systemId == null || systemId.isBlank()) {
      throw new IllegalArgumentException("Invalid system id: " + systemId);
    }
    return new SystemId(systemId);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof SystemId)) return false;
    return value.equals(((SystemId) other).value);
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
    return getValue();
  }
}
