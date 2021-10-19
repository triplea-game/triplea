package org.triplea.java.cache;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This is a "forgetful" cache. Data becomes inactive after a TTL (time-to-live) expires, inactive
 * data is removed from the cache and forgotton about. Data can be kept in the cache by periodically
 * and explicitly invoking 'refresh' on the cache to keep the data in-memory until the next TTL.
 *
 * <p>This useful for example when we need maintain client data and clients can drop-off. To avoid
 * retaining the data indefinetely, it expires with TTL if a client disconnects and stops
 * refreshing.
 *
 * @param <IdT> This is an ID for the key-value cache. The ID alone is used to 'refresh' the cache
 *     entry.
 * @param <ValueT> This is the cached value, keyed by an ID.
 */
public interface TtlCache<IdT, ValueT> {

  /**
   * Extends the 'life' of a given entry and prevents cache expiration for another TTL.
   *
   * @return True if an element existed and was granted another TTL. False indicates the refresh is
   *     too late and the item expired and was removed (or never existed to begin with).
   */
  boolean refresh(IdT id);

  /** Retrieves a value from the cache, if any. */
  Optional<ValueT> get(IdT id);

  /** Places a new item in the cache, overwriting any values with the existing ID. */
  void put(IdT id, ValueT value);

  /** Explicitly removes an item from cache, returns any such value that was removed. */
  Optional<ValueT> invalidate(IdT id);

  /**
   * Places a new item in the cache if one existed with the same ID and replaces the existing value.
   * The replaced item is returned, otherwise returns an empty.
   */
  Optional<ValueT> replace(IdT id, ValueT newValue);

  default Optional<Map.Entry<IdT, ValueT>> findEntryByKey(final Predicate<IdT> keyCheck) {
    return asMap().entrySet().stream().filter(entry -> keyCheck.test(entry.getKey())).findAny();
  }

  /**
   * Returns a snapshot (copy) of all values currently in the cache. Note, modifications to the
   * returned {@code Map} will *not* update the underlying cache.
   */
  Map<IdT, ValueT> asMap();
}
