package org.triplea.live.servers;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.test.common.StringToInputStream.asInputStream;

import games.strategy.engine.framework.map.download.CloseableDownloader;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.TestDataFileReader;

/**
 * End-to-end to Parses an example servers.yml file and the current servers.yaml file (from local
 * file system) and verifies we can extract data from it.
 */
class LiveServersFetcherIntegrationTest extends AbstractClientSettingTestCase {

  @Test
  @DisplayName("End-to-end test parsing a sample configuration file")
  void verifyConfiguration() {
    final LiveServersFetcher liveServersFetcher =
        new LiveServersFetcher(() -> givenYaml("live.servers.yaml.examples/servers_example.yaml"));

    assertThat(liveServersFetcher.latestVersion(), isPresent());
    assertThat(liveServersFetcher.lobbyUriForCurrentVersion(), isPresent());
    assertThat(liveServersFetcher.serverForCurrentVersion(), notNullValue());
  }

  private CloseableDownloader givenYaml(final String source) {
    return new CloseableDownloader() {
      @Override
      public InputStream getStream() {
        return asInputStream(TestDataFileReader.readContents(source));
      }

      @Override
      public void close() {
        // no-op
      }
    };
  }

  @Test
  @DisplayName("End-to-end test parsing the current servers.yml file")
  void verifyCurrentConfiguration() {
    final LiveServersFetcher liveServersFetcher =
        new LiveServersFetcher(() -> givenYaml("servers.yml"));
    assertThat(liveServersFetcher.latestVersion(), isPresent());
    assertThat(liveServersFetcher.lobbyUriForCurrentVersion(), isPresent());
    assertThat(liveServersFetcher.serverForCurrentVersion(), notNullValue());
  }
}
