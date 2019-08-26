package org.triplea.server.http;

import javax.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class to extract the remote host machine IP address from an http servlet request. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IpAddressExtractor {

  /**
   * Extracts remote host IP from parameter. This will be the IP of the client making
   * the {@code HttpServerRequest}.
   */
  public static String extractClientIp(final HttpServletRequest request) {
    return request.getRemoteAddr();
  }
}
