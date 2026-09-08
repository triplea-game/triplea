package org.triplea.java.cache.ttl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExpiringAfterWriteTtlCacheTest {

  @NonNls private static final String KEY = "key-value";
  private static final int VALUE = 100;
  private ExpiringAfterWriteTtlCache<String, Integer> realCache;

  private String cacheRemovalKey;
  private Integer cacheRemovalValue;
  private final BiConsumer<String, Integer> cacheRemovalListener =
      (key, value) -> {
        cacheRemovalKey = key;
        cacheRemovalValue = value;
      };

  @BeforeEach
  void setUp() {
    realCache = new ExpiringAfterWriteTtlCache<>(1, TimeUnit.MINUTES, cacheRemovalListener);
  }

  @Nested
  class GetAndPut {
    @Test
    void getWillReturnValuesThatHaveBeenPutIntoCache() {
      realCache.put("id", 0);
      assertThat(realCache.get("id")).contains(0);
    }

    @Test
    void getReturnsEmptyIfValueDoesNotExist() {
      assertThat(realCache.get("DNE")).isEmpty();
    }

    @Test
    void putCanBeUsedToReplace() {
      realCache.put("id", 0);

      realCache.put("id", 1);

      assertThat(realCache.get("id")).contains(1);
      assertThat(cacheRemovalKey).isNull();
      assertThat(cacheRemovalValue).isNull();
    }
  }

  @Nested
  class Replace {

    @Test
    @DisplayName("When replacing a value that does not exist, no value should be returned")
    void emptyReplaceDoesNotReturnValues() {
      final Optional<Integer> result = realCache.replace("id0", 0);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("When replacing a value that does not exist, no value should be added")
    void emptyReplaceDoesNotAddValues() {
      realCache.replace("id0", 0);

      final Optional<Integer> result = realCache.get("id0");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Replacing a value replaces the value")
    void replaceWillReplaceAnExistingValue() {
      realCache.put("id1", 0);

      realCache.replace("id1", 1);
      final Optional<Integer> result = realCache.get("id1");

      assertThat(result).contains(1);
    }

    @Test
    @DisplayName("Replacing an existing value will return the replaced value")
    void replaceWillReturnTheReplacedValue() {
      realCache.put("id2", 0);

      final Optional<Integer> result = realCache.replace("id2", 1);

      assertThat(result).contains(0);
    }

    @Test
    void removalListenerIsNotInvokedWhenAnItemIsReplaced() {
      realCache.put("id20", 0);

      realCache.replace("id20", 1);

      assertThat(cacheRemovalKey).isNull();
      assertThat(cacheRemovalValue).isNull();
    }
  }

  @Nested
  class Invalidate {
    @Test
    void invalidateRemovesAnItemFromCache() {
      realCache.put("id3", 0);

      realCache.invalidate("id3");

      assertThat(realCache.get("id3")).isEmpty();
    }

    @Test
    void invalideReturnsInvalidatedValue() {
      realCache.put("id4", 0);

      final Optional<Integer> result = realCache.invalidate("id4");

      assertThat(result).contains(0);
    }

    @Test
    void invalidateReturnsEmptyIfValueDoesNotExist() {
      final Optional<Integer> result = realCache.invalidate("DNE");

      assertThat(result).isEmpty();
    }

    @Test
    void removalListenerIsInvokedWhenItemsAreInvalidated() {
      realCache.put("id10", 0);
      realCache.invalidate("id10");

      assertThat(cacheRemovalKey).isEqualTo("id10");
      assertThat(cacheRemovalValue).isEqualTo(0);
    }
  }

  @Nested
  class FindEntryByKey {
    @Test
    void emptyCase() {
      assertThat(realCache.findEntryByKey(key -> key.equals(KEY))).isEmpty();
    }

    @Test
    void notFoundCase() {
      realCache.put(KEY, VALUE);
      assertThat(realCache.findEntryByKey(key -> key.equals("some-other-key"))).isEmpty();
    }

    @Test
    void foundCase() {
      realCache.put(KEY, VALUE);

      final Optional<Map.Entry<String, Integer>> result =
          realCache.findEntryByKey(key -> key.equals(KEY));

      assertThat(result.isPresent()).isTrue();
      assertThat(result.get().getKey()).isEqualTo(KEY);
      assertThat(result.get().getValue()).isEqualTo(VALUE);
    }
  }

  @Nested
  class Refresh {

    @Test
    void refreshFalseIfNotInCacheEmptyCase() {
      final boolean result = realCache.refresh(KEY);

      assertThat(result).isFalse();
    }

    @Test
    void refreshFalseIfNotInCacheNotFoundCase() {
      realCache.put(KEY, VALUE);

      final boolean result = realCache.refresh("wrong-key-value");

      assertThat(result).isFalse();
    }

    @Test
    void refreshTrueWhenFoundInCache() {
      realCache.put(KEY, VALUE);

      final boolean result = realCache.refresh(KEY);

      assertThat(result).isTrue();
    }
  }
}
