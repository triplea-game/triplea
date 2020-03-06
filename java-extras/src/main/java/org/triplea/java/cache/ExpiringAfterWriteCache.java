package org.triplea.java.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;

/**
 * Cache that expires values when a TTL (time to live) expires. TTL timer starts when the value is
 * written and is renewed if the value is 'refreshed'. The cache will reliably invoke a
 * 'removeListener' at least once when cache items are removed or expired.
 *
 * @param <IdT> Type that identifies the keys of the map.
 * @param <ValueT> Type that is placed as a value in the map.
 */
public class ExpiringAfterWriteCache<IdT, ValueT> implements TtlCache<IdT, ValueT> {

  private final Cache<IdT, ValueT> cache;
  private final ScheduledTimer cleanupTimer;
  private final Consumer<CacheEntry<IdT, ValueT>> removalListener;

  public ExpiringAfterWriteCache(
      final long duration,
      final TimeUnit timeUnit,
      final Consumer<CacheEntry<IdT, ValueT>> removalListener) {
    cache =
        Caffeine.newBuilder()
            .expireAfterWrite(duration, timeUnit)
            .removalListener(
                (IdT key, ValueT value, RemovalCause cause) -> {
                  if (cause == RemovalCause.EXPIRED || cause == RemovalCause.EXPLICIT) {
                    removalListener.accept(new CacheEntry<>(key, value));
                  }
                })
            .build();

    this.removalListener = removalListener;

    cleanupTimer =
        Timers.fixedRateTimer("cache-cleanup-" + Math.random())
            .period(1000, TimeUnit.MILLISECONDS)
            .task(cache::cleanUp)
            .start();
  }

  public void close() {
    cleanupTimer.cancel();
  }

  @Override
  public boolean refresh(final IdT id) {
    final Optional<ValueT> value = get(id);

    if (value.isPresent()) {
      put(id, value.get());
      return true;
    }

    return false;
  }

  @Override
  public Optional<ValueT> get(final IdT id) {
    return Optional.ofNullable(cache.getIfPresent(id));
  }

  @Override
  public void put(final IdT id, final ValueT value) {
    cache.put(id, value);
  }

  @Override
  public Optional<ValueT> invalidate(final IdT id) {
    final Optional<ValueT> value = get(id);
    cache.invalidate(id);
    value.ifPresent(valueT -> removalListener.accept(new CacheEntry<>(id, valueT)));
    return value;
  }

  @Override
  public Map<IdT, ValueT> asMap() {
    return Map.copyOf(cache.asMap());
  }
}
