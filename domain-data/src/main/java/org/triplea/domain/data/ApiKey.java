package org.triplea.domain.data;

import com.google.common.base.Preconditions;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * An OAuth token, represents an authorization token passed to client from server on successful
 * authentication. The token is passed back to the server on subsequent requests to prove
 * authorization.
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiKey {
  public static final int MAX_LENGTH = 36;

  private final String value;

  public static ApiKey newKey() {
    return of(UUID.randomUUID().toString());
  }

  public static ApiKey of(final String value) {
    Preconditions.checkArgument(value != null);
    Preconditions.checkArgument(!value.isEmpty());
    Preconditions.checkArgument(!value.contains("\n"));
    Preconditions.checkArgument(value.length() <= MAX_LENGTH);

    return new ApiKey(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
