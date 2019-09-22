package org.triplea.server.access;

import lombok.Value;

/** Simple value object for strong typing. */
@Value(staticConstructor = "of")
public class ApiKey {
  private final String value;
}
