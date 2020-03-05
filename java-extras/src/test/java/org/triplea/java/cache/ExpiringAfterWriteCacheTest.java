package org.triplea.java.cache;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpiringAfterWriteCacheTest {

  private static final String KEY = "key-value";
  private static final int VALUE = 100;
  private ExpiringAfterWriteCache<String, Integer> realCache;

  @Mock private Consumer<TtlCache.CacheEntry<String, Integer>> cacheRemovalListener;

  @BeforeEach
  void setup() {
    realCache = new ExpiringAfterWriteCache<>(1, TimeUnit.SECONDS, cacheRemovalListener);
  }

  @AfterEach
  void tearDown() {
    realCache.stopTimer();
  }

  @Nested
  class FindEntryByKey {

    @Test
    void emptyCase() {
      assertThat(realCache.findEntryByKey(key -> key.equals(KEY)), isEmpty());
    }

    @Test
    void notFoundCase() {
      realCache.put(KEY, VALUE);
      assertThat(realCache.findEntryByKey(key -> key.equals("some-other-key")), isEmpty());
    }

    @Test
    void foundCase() {
      realCache.put(KEY, VALUE);

      final Optional<Map.Entry<String, Integer>> result =
          realCache.findEntryByKey(key -> key.equals(KEY));

      assertThat(result.isPresent(), is(true));
      assertThat(result.get().getKey(), is(KEY));
      assertThat(result.get().getValue(), is(VALUE));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  class Refresh {
    @Mock private Cache<String, Integer> mockCache;

    @Test
    void refreshFalseIfNotInCacheEmptyCase() {
      final boolean result = realCache.refresh(KEY);

      assertThat(result, is(false));
    }

    @Test
    void refreshFalseIfNotInCacheNotFoundCase() {
      realCache.put(KEY, VALUE);

      final boolean result = realCache.refresh("wrong-key-value");

      assertThat(result, is(false));
    }

    @Test
    void refreshTrueWhenFoundInCache() {
      realCache.put(KEY, VALUE);

      final boolean result = realCache.refresh(KEY);

      assertThat(result, is(true));
    }

    @Test
    void refreshedItemsAreWrittenBackIntoTheCache() {
      when(mockCache.getIfPresent(KEY)).thenReturn(VALUE);

      final boolean result = realCache.refresh(KEY);

      assertThat(result, is(true));
      verify(mockCache).put(KEY, VALUE);
    }
  }
}
