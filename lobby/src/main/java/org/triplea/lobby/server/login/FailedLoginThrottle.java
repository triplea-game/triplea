package org.triplea.lobby.server.login;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Class to enforce login throttling if there are too many failed login attempts. This is to prevent
 * brute-force attacks attempting to crack a users password.
 */
public class FailedLoginThrottle {
  /**
   * How many failed login attempts per time period that can be attempted before we outright reject
   * any further login attempts.
   */
  @VisibleForTesting static final int MAX_FAILED_LOGIN_ATTEMPTS = 7;

  /**
   * How much time (in minutes) must be waited before user can attempt a login after having too many
   * failed login attempts.
   */
  private static final int FAILED_LOGIN_COOLOFF = 3;

  /**
   * Cache that maps inet address to failed login attempts. This is to keep track of failed login
   * attempts and eventaully lock a user out (and prevent a brute-force password cracking attempt).
   */
  private final Cache<InetAddress, Integer> failedLoginCache =
      CacheBuilder.newBuilder().expireAfterWrite(FAILED_LOGIN_COOLOFF, TimeUnit.MINUTES).build();

  boolean tooManyFailedLoginAttempts(final InetAddress address) {
    final int failedAttempts =
        Optional.ofNullable(failedLoginCache.getIfPresent(address)).orElse(0) + 1;
    return failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS;
  }

  void increment(final InetAddress address) {
    final int failedAttempts =
        Optional.ofNullable(failedLoginCache.getIfPresent(address)).orElse(0) + 1;
    failedLoginCache.put(address, failedAttempts);
  }
}
