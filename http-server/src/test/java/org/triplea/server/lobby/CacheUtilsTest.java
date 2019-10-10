package org.triplea.server.lobby;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"InnerClassMayBeStatic", "OptionalGetWithoutIsPresent"})
class CacheUtilsTest {

  private static final String KEY = "key-value";
  private static final int VALUE = 100;
  private Cache<String, Integer> realCache = CacheBuilder.newBuilder().maximumSize(1).build();

  @Nested
  class FindEntryByKey {

    @Test
    void emptyCase() {
      assertThat(CacheUtils.findEntryByKey(realCache, key -> key.equals(KEY)), isEmpty());
    }

    @Test
    void notFoundCase() {
      realCache.put(KEY, VALUE);
      assertThat(
          CacheUtils.findEntryByKey(realCache, key -> key.equals("some-other-key")), isEmpty());
    }

    @Test
    void foundCase() {
      realCache.put(KEY, VALUE);

      final Optional<Map.Entry<String, Integer>> result =
          CacheUtils.findEntryByKey(realCache, key -> key.equals(KEY));

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
      final boolean result = CacheUtils.refresh(realCache, KEY);

      assertThat(result, is(false));
    }

    @Test
    void refreshFalseIfNotInCacheNotFoundCase() {
      realCache.put(KEY, VALUE);

      final boolean result = CacheUtils.refresh(realCache, "wrong-key-value");

      assertThat(result, is(false));
    }

    @Test
    void refreshTrueWhenFoundInCache() {
      realCache.put(KEY, VALUE);

      final boolean result = CacheUtils.refresh(realCache, KEY);

      assertThat(result, is(true));
    }

    @Test
    void refreshedItemsAreWrittenBackIntoTheCache() {
      when(mockCache.getIfPresent(KEY)).thenReturn(VALUE);

      final boolean result = CacheUtils.refresh(mockCache, KEY);

      assertThat(result, is(true));
      verify(mockCache).put(KEY, VALUE);
    }
  }
}
