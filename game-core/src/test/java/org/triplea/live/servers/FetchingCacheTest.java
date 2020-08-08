package org.triplea.live.servers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.framework.map.download.CloseableDownloader;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Function;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.function.ThrowingSupplier;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class FetchingCacheTest extends AbstractClientSettingTestCase {

  @Mock private InputStream inputStream;
  @Mock private LiveServers liveServers;

  @Mock private ThrowingSupplier<CloseableDownloader, IOException> closeableDownloaderFactory;
  @Mock private CloseableDownloader closeableDownloader;
  @Mock private Function<InputStream, LiveServers> yamlParser;

  private FetchingCache fetchingCache;

  @BeforeEach
  void setUp() {
    FetchingCache.liveServersCache = null;
    fetchingCache =
        FetchingCache.builder()
            .contentDownloader(closeableDownloaderFactory)
            .yamlParser(yamlParser)
            .build();
  }

  @Test
  void exceptionsArePassedUpTheStack() throws Exception {
    when(closeableDownloaderFactory.get()).thenThrow(new IOException("simulated exception"));

    assertThrows(IOException.class, () -> fetchingCache.get());
  }

  @Test
  @DisplayName("Verify when there is a cache miss, we call invoke network fetch and parser")
  void cacheMissCallsFetcherAndParser() throws Exception {
    givenMocksWillReturnParsedLiveServers();

    final LiveServers result = fetchingCache.get();

    assertThat(result, sameInstance(liveServers));
  }

  void givenMocksWillReturnParsedLiveServers() throws Exception {
    when(closeableDownloaderFactory.get()).thenReturn(closeableDownloader);
    when(closeableDownloader.getStream()).thenReturn(inputStream);
    when(yamlParser.apply(inputStream)).thenReturn(liveServers);
  }

  @Test
  @DisplayName("Verify when there is a cache value, we do not call network or parser")
  void cacheHitCase() throws Exception {
    givenCacheIsPopulated();

    fetchingCache.get();

    verify(closeableDownloaderFactory, never()).get();
    verify(closeableDownloader, never()).getStream();
    verify(yamlParser, never()).apply(any());
  }

  private void givenCacheIsPopulated() {
    FetchingCache.liveServersCache = liveServers;
  }

  @Test
  @DisplayName("Verify after we fill the cache, we no longer call network or parser")
  void cacheIsUsedWhenNotEmpty() throws Exception {
    givenMocksWillReturnParsedLiveServers();

    fetchingCache.get();
    fetchingCache.get();

    // make sure we made exactly one call
    verify(closeableDownloaderFactory).get();
    verify(closeableDownloader).getStream();
    verify(yamlParser).apply(any());
  }

  @Test
  void lobbyUriOverride() throws Exception {
    final URI overrideUri = URI.create("http://lobby.overrride");
    ClientSetting.lobbyUriOverride.setValueAndFlush(overrideUri);

    final LiveServers result = fetchingCache.get();

    assertThat(result.getServers(), IsCollectionWithSize.hasSize(1));
    assertThat(result.getServers().get(0).getUri(), is(overrideUri));
  }
}
