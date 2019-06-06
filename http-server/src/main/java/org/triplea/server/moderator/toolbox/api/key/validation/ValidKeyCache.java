package org.triplea.server.moderator.toolbox.api.key.validation;


import java.util.Optional;

/**
 * A cache where we will remember valid API keys. If we are in lock-out mode, then we'll use this cache
 * to remember already validated API keys and will not lock-out authenticated API keys that are in the cache.
 * Thus in lock-out mode only new API keys will not authenticated but existing ones will continue to work.
 * Then intent is to prevent a DDOS attack of any existing moderators that have already authenticated.
 */
class ValidKeyCache {
  Optional<Integer> get(final String apiKey) {
    return Optional.empty();
  }

  void recordValid(final String apiKey, final int moderatorId) {

  }
}
