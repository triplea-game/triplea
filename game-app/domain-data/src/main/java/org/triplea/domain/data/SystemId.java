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
    if (other instanceof SystemId systemId) return value.equals(systemId.value);
    if (other instanceof String string) return value.equals(string);
    return false;
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
