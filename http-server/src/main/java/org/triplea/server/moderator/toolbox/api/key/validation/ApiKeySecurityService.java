package org.triplea.server.moderator.toolbox.api.key.validation;

import javax.servlet.http.HttpServletRequest;

/**
 * Verifies rate limiting to prevent brute-force cracking of moderator API key. This is done
 * by limiting number of bad tries by IP address per unit time and by limiting the number of bad
 * tries across all IP address per unit per time.
 * <p>
 * Recording of validation attempts is stored in memory. This is to help prevent DDOS-like attacks from
 * accessing database.
 * </p>
 */
public class ApiKeySecurityService {

  // TODO: implement-me
  public boolean allowValidation(final HttpServletRequest servletRequest) {
    return true;
  }
}
