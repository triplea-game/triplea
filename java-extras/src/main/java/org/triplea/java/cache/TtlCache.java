package org.triplea.java.cache;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface TtlCache<IdT, ValueT> {

  /**
   * Extends the 'life' of a given entry and prevents cache expiration for another TTL. Returns true
   * if an element existed and was refreshed, returns false if no such element existed.
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
