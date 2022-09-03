package org.triplea.dropwizard.common;

import javax.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IpAddressExtractor {

  /**
   * Extracts the IP address of the remote address making an HttpServletRequest.
   *
   * <p>httpServletRequest.getRemoteAddr() can return a value surrounded by square brackets. This
   * method will return that remote addr with square brackets stripped.
   *
   * @return valid IP address of the remote machine making
   */
  public String extractIpAddress(HttpServletRequest httpServletRequest) {
    return httpServletRequest
        .getRemoteAddr() //
        .replaceAll("\\[", "")
        .replaceAll("\\]", "");
  }
}
