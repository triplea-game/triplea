package org.triplea.domain.data;

import com.google.common.base.Preconditions;
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
    Preconditions.checkNotNull(systemId);
    Preconditions.checkArgument(!systemId.isEmpty());
    return new SystemId(systemId);
  }

  @Override
  public String toString() {
    return getValue();
  }
}
