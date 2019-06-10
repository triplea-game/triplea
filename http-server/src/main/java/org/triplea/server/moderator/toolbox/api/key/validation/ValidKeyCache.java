package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A cache where we will remember valid API keys. If we are in lock-out mode, then we'll use this cache
 * to remember already validated API keys and will not lock-out authenticated API keys that are in the cache.
 * Thus in lock-out mode only new API keys will not authenticated but existing ones will continue to work.
 * Then intent is to prevent a DDOS attack of any existing moderators that have already authenticated.
 */
class ValidKeyCache {

  private static final Cache<String, Integer> validKeys = CacheBuilder.newBuilder()
      .expireAfterWrite(1, TimeUnit.DAYS)
      .build();

  Optional<Integer> get(final String apiKey) {
    return Optional.ofNullable(validKeys.getIfPresent(apiKey));
  }

  void recordValid(final String apiKey, final int moderatorId) {
    validKeys.put(apiKey, moderatorId);
  }
}
