package org.triplea.http.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NonNls;

/** Utility class with constants for HTTP clients. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpConstants {
  public static final String ACCEPT_JSON = "Accept: application/json";
  @NonNls public static final String CONTENT_TYPE_JSON = "Content-Type: application/json";
}
