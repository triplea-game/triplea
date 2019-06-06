package org.triplea.server.http;

import javax.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class to extract the remote host machine IP address from an http servlet request.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IpAddressExtractor {


  static final String XFORWARDED_HEADER = "X-Forwarded-For";

  /**
   * Exracts remote host IP from either X-Forwarded-For header, and if not present, extracts
   * the IP address from the request object.
   *
   * @param request Request object containing remote host IP and/or X-Forwarded-For header.
   */
  public static String extractClientIp(final HttpServletRequest request) {
    final String forwarded = request.getHeader(XFORWARDED_HEADER);
    if (forwarded != null) {
      return forwarded;
    }
    return request.getRemoteHost();
  }
}
