package org.triplea.server.moderator.toolbox.api.key.validation;

import javax.servlet.http.HttpServletRequest;

/**
 * Class to determine if API key validation is in a 'lock-out' mode where we will refuse to validate
 * API keys and will refuse the request. This is intended to prevent brute-force attacks from attempting
 * to guess an API key.
 */
class InvalidKeyLockOut {

  /**
   * Returns true if no attempts should be made to validate the API key in a given request. Returns false
   * if we can continue with a database lookup to validate an API key.
   * 
   * @param request Request containing moderator api key as a header.
   */
  boolean isLockedOut(final HttpServletRequest request) {
    return false;
  }

  void recordInvalid(final HttpServletRequest request) {
  }
}
