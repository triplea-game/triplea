package org.triplea.http.client;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Simple value object for strong typing. */
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiKey {
  private static final int DB_COLUMN_LENGTH = 256;
  private final String value;

  public static ApiKey of(final String value) {
    Preconditions.checkArgument(value != null);
    Preconditions.checkArgument(!value.isEmpty());
    Preconditions.checkArgument(value.length() <= DB_COLUMN_LENGTH);
    Preconditions.checkArgument(!value.contains("\n"));

    return new ApiKey(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
