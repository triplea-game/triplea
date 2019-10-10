package org.triplea.server.lobby;

import com.google.common.cache.Cache;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/**
 * Utility class to provide common operations around a guava cache, notably provides a 'refresh'
 * method used for keep-alive caches.
 */
@UtilityClass
public class CacheUtils {

  public <X, Y> Optional<Map.Entry<X, Y>> findEntryByKey(
      final Cache<X, Y> cache, final Predicate<X> keyCheck) {
    return cache.asMap().entrySet().stream()
        .filter(entry -> keyCheck.test(entry.getKey()))
        .findAny();
  }

  public <X, Y> boolean refresh(final Cache<X, Y> cache, final X key) {
    return Optional.ofNullable(cache.getIfPresent(key))
        .map(
            value -> {
              cache.put(key, value);
              return true;
            })
        .orElse(false);
  }
}
