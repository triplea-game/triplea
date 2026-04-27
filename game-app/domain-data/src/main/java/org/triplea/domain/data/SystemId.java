package org.triplea.domain.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Identifier generated on a users system to uniquely identify that player in addition to their IP
 * address.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
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
  public String toString() {
    return getValue();
  }
}
