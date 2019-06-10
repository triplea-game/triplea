package org.triplea.server.moderator.toolbox.api.key.validation;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Preconditions;

import lombok.Builder;

/**
 * Class to determine if API key validation is in a 'lock-out' mode where we will refuse to validate
 * API keys and will refuse the request. This is intended to prevent brute-force attacks from attempting
 * to guess an API key.
 */
class InvalidKeyLockOut {

  @Nonnull
  private final InvalidKeyCache invalidKeyCache;
  @Nonnull
  private final Integer maxTotalFails;
  @Nonnull
  private final Integer maxFailsByIpAddress;

  @Builder
  InvalidKeyLockOut(
      @Nonnull final InvalidKeyCache invalidKeyCache,
      final int maxTotalFails,
      final int maxFailsByIpAddress) {
    Preconditions.checkNotNull(invalidKeyCache);
    Preconditions.checkArgument(maxTotalFails >= maxFailsByIpAddress);
    Preconditions.checkArgument(maxFailsByIpAddress > 0);
    this.invalidKeyCache = invalidKeyCache;
    this.maxTotalFails = maxTotalFails;
    this.maxFailsByIpAddress = maxFailsByIpAddress;
  }

  /**
   * Returns true if no attempts should be made to validate the API key in a given request.
   * Returns false to indicate there is no lockout and if we can continue further
   * with a database lookup to validate an API key.
   *
   * @param request Request containing moderator api key as a header.
   */
  synchronized boolean isLockedOut(final HttpServletRequest request) {
    return invalidKeyCache.getCount(request) >= maxFailsByIpAddress
        || invalidKeyCache.totalSum() >= maxTotalFails;
  }

  void recordInvalid(final HttpServletRequest request) {
    invalidKeyCache.increment(request);
  }
}
