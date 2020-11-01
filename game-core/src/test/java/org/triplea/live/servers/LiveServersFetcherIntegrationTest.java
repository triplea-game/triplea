package org.triplea.live.servers;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.when;
import static org.triplea.test.common.StringToInputStream.asInputStream;

import games.strategy.engine.framework.map.download.CloseableDownloader;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.test.common.Integration;
import org.triplea.test.common.TestDataFileReader;

/**
 * End-to-end to Parses an example servers.yml file and the current servers.yaml file (from local
 * file system) and verifies we can extract data from it.
 */
@Integration
@ExtendWith(MockitoExtension.class)
class LiveServersFetcherIntegrationTest extends AbstractClientSettingTestCase {

  @Mock private CloseableDownloader closeableDownloader;

  @BeforeEach
  void setUp() {
    // clear cache
    FetchingCache.liveServersCache = null;
  }

  @Test
  @DisplayName("End-to-end test parsing a sample configuration file")
  void verifyConfiguration() {
    givenInputStreamFromFile("live.servers.yaml.examples/servers_example.yaml");

    final LiveServersFetcher liveServersFetcher =
        new LiveServersFetcher(() -> closeableDownloader, new ProductVersionReader().getVersion());

    assertThat(liveServersFetcher.latestVersion(), isPresent());
    assertThat(liveServersFetcher.lobbyUriForCurrentVersion(), isPresent());
    assertThat(liveServersFetcher.serverForCurrentVersion(), notNullValue());
  }

  private void givenInputStreamFromFile(final String file) {
    when(closeableDownloader.getStream())
        .thenReturn(asInputStream(TestDataFileReader.readContents(file)));
  }

  @Test
  @DisplayName("End-to-end test parsing the current servers.yml file")
  void verifyCurrentConfiguration() {
    givenInputStreamFromFile("servers.yml");

    final LiveServersFetcher liveServersFetcher =
        new LiveServersFetcher(() -> closeableDownloader, new ProductVersionReader().getVersion());

    assertThat(liveServersFetcher.latestVersion(), isPresent());
    assertThat(liveServersFetcher.lobbyUriForCurrentVersion(), isPresent());
    assertThat(liveServersFetcher.serverForCurrentVersion(), notNullValue());
  }
}
