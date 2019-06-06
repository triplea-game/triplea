package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.server.moderator.toolbox.api.key.validation.exception.ApiKeyVerificationLockOutException;
import org.triplea.server.moderator.toolbox.api.key.validation.exception.IncorrectApiKeyException;

import com.google.common.base.Preconditions;

import lombok.Builder;


/**
 * Performs rate limiting and moderator API key validation. Rate limiting can be done against a single IP
 * address or against all IP address if there are too many failed attempts. It's important that when we do
 * rate-limiting we do not access database to help avoid a DDOS attack against the system.
 */
@Builder
public class ApiKeyValidationService {
  @Nonnull
  private final ValidKeyCache validKeyCache;
  @Nonnull
  private final InvalidKeyLockOut invalidKeyLockOut;
  @Nonnull
  private final Function<String, Optional<Integer>> apiKeyLookup;


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
   *
   * @param request Server request object expected to contain an API key header.
   * @return The moderator database ID that matches the API key. Otherwise an exception will be thrown.
   *
   * @throws IllegalArgumentException Thrown if the http servlet request headers do not contain a moderator api key.
   * @throws IncorrectApiKeyException Thrown if the provided API key does not match any known keys.
   * @throws ApiKeyVerificationLockOutException Thrown if rate-limiting has kicked in and the API key verification
   *         was not attempted. This can be caused by too many attempts from a specific IP address, or too many failed
   *         attempts across all IP addresses. To avoid the latter case from locking all users out, valid API keys
   *         are cached and will by-pass the lock-out if we enter into that state. We do a lock-out for all
   *         IP addresses in case an attacker sets up many computers or is able to spoof their IP address.
   */
  public int lookupModeratorIdByApiKey(final HttpServletRequest request) {
    final String apiKey = request.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER);
    Preconditions.checkArgument(apiKey != null && !apiKey.isEmpty());

    final Optional<Integer> cacheResult = validKeyCache.get(apiKey);
    if (cacheResult.isPresent()) {
      return cacheResult.get();
    }

    if (invalidKeyLockOut.isLockedOut(request)) {
      throw new ApiKeyVerificationLockOutException();
    }

    final Optional<Integer> lookupResult = apiKeyLookup.apply(apiKey);

    if (lookupResult.isPresent()) {
      validKeyCache.recordValid(apiKey, lookupResult.get());
      return lookupResult.get();
    }

    invalidKeyLockOut.recordInvalid(request);
    throw new IncorrectApiKeyException();
  }
}
