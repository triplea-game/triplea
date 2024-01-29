package org.triplea.http.client;

import lombok.experimental.UtilityClass;

/** Class to hold constants that need to be shared between multiple different http-clients. */
@UtilityClass
public class HttpClientConstants {
  /**
   * Arbitrary length to prevent titles from being too large and cluttering up the issue display.
   */
  public static final int TITLE_MAX_LENGTH = 125;

  /** Max length for github issue body text. */
  public static final int REPORT_BODY_MAX_LENGTH = 65536;
}
