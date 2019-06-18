package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.Optional;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.server.http.IpAddressExtractor;
import org.triplea.server.moderator.toolbox.api.key.InvalidKeyLockOut;
import org.triplea.server.moderator.toolbox.api.key.exception.ApiKeyLockOutException;
import org.triplea.server.moderator.toolbox.api.key.exception.IncorrectApiKeyException;

import com.google.common.base.Preconditions;

import lombok.Builder;
import lombok.extern.java.Log;


/**
 * Performs rate limiting and moderator API key validation. Rate limiting can be done against a single IP
 * address or against all IP address if there are too many failed attempts. It's important that when we do
 * rate-limiting we do not access database to help avoid a DDOS attack against the system.
 */
@Log
@Builder
public class ApiKeyValidationService {
  @Nonnull
  private final ValidKeyCache validKeyCache;
  @Nonnull
  private final InvalidKeyLockOut invalidKeyLockOut;
  @Nonnull
  private final BiFunction<String, String, String> keyHasher;
  @Nonnull
  private final ModeratorApiKeyDao moderatorApiKeyDao;


  /**
   * Convenience method to verify a valid moderator Api-key is present in headers, otherwise an exception is thrown.
   *
   * @see #lookupModeratorIdByApiKey
   */
  public void verifyApiKey(final HttpServletRequest request) {
    lookupModeratorIdByApiKey(request);
  }

  /**
   * Does a lookup of API key contained in request header and returns the user id of
   * the moderator that owns that key. If no key is found, or if verification is locked down, or no key
   * is present, then an exception is thrown.
   * Synchronized so that the check for locked out request and then incrementing the failed
   * request count is atomic.
   *
   * @param request Server request object expected to contain an API key header.
   * @return The moderator database ID that matches the API key. Otherwise an exception will be thrown.
   *
   * @throws IllegalArgumentException Thrown if the http servlet request headers do not contain a moderator api key.
   * @throws IncorrectApiKeyException Thrown if the provided API key does not match any known keys.
   * @throws ApiKeyLockOutException Thrown if rate-limiting has kicked in and the API key verification
   *         was not attempted. This can be caused by too many attempts from a specific IP address, or too many failed
   *         attempts across all IP addresses. To avoid the latter case from locking all users out, valid API keys
   *         are cached and will by-pass the lock-out if we enter into that state. We do a lock-out for all
   *         IP addresses in case an attacker sets up many computers or is able to spoof their IP address.
   */
  public synchronized int lookupModeratorIdByApiKey(final HttpServletRequest request) {
    final String hashedKey = extractHashedKey(request);

    final Optional<Integer> cacheResult = validKeyCache.get(hashedKey);
    if (cacheResult.isPresent()) {
      return cacheResult.get();
    }

    if (invalidKeyLockOut.isLockedOut(request)) {
      throw new ApiKeyLockOutException();
    }

    final Optional<Integer> lookupResult = moderatorApiKeyDao.lookupModeratorIdByApiKey(hashedKey);

    if (lookupResult.isPresent()) {
      validKeyCache.recordValid(hashedKey, lookupResult.get());
      Preconditions.checkState(moderatorApiKeyDao.recordKeyUsage(hashedKey, request.getRemoteAddr()) == 1);
      final int moderatorId = lookupResult.get();
      log.info("API Key for moderator ID: " + moderatorId + " validated successfully.");
      return moderatorId;
    }

    invalidKeyLockOut.recordInvalid(request);
    log.warning("API key authentication failed for IP: " + IpAddressExtractor.extractClientIp(request));
    throw new IncorrectApiKeyException();
  }

  private String extractHashedKey(final HttpServletRequest request) {
    final String apiKey = request.getHeader(ToolboxHttpHeaders.API_KEY_HEADER);
    Preconditions.checkArgument(apiKey != null && !apiKey.isEmpty());

    final String apiKeyPassword = request.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER);
    Preconditions.checkArgument(apiKeyPassword != null && !apiKeyPassword.isEmpty());

    return keyHasher.apply(apiKey, apiKeyPassword);
  }

  public int verifySuperMod(final HttpServletRequest request) {
    return lookupSuperModByApiKey(request).orElseThrow(IncorrectApiKeyException::new);
  }

  public Optional<Integer> lookupSuperModByApiKey(final HttpServletRequest request) {
    final String hashedKey = extractHashedKey(request);

    return moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(hashedKey);
  }

  void clearLockoutCache(final HttpServletRequest request) {
    invalidKeyLockOut.clearLockouts(request);
  }
}
