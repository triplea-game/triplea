package org.triplea.live.servers;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.live.servers.LiveServersFetcher.LobbyAddressFetchException;
import org.triplea.util.Version;

@ExtendWith(MockitoExtension.class)
class LiveServersFetcherTest {
  private static final URI LOBBY_URI = URI.create("https://dummy.uri");

  @Mock private LiveServers liveServers;
  @Mock private ServerProperties serverProperties;

  @Mock private ThrowingSupplier<LiveServers, IOException> networkFetcher;
  @Mock private Function<LiveServers, ServerProperties> currentVersionSelector;

  private LiveServersFetcher liveServersFetcher;

  @BeforeEach
  void setUp() {
    liveServersFetcher = new LiveServersFetcher(currentVersionSelector, networkFetcher);
  }

  @Nested
  class LatestVersion {
    @Test
    @DisplayName("Verify latest version returning a value")
    void latestVersionPresent() throws Exception {
      when(networkFetcher.get())
          .thenReturn(
              LiveServers.builder()
                  .latestEngineVersion(new Version("10.0"))
                  .servers(List.of())
                  .build());

      final Optional<Version> version = liveServersFetcher.latestVersion();

      assertThat(version, isPresentAndIs(new Version("10.0")));
    }

    @Test
    @DisplayName("Verify latest version returns empty after network fetch exception")
    void latestVersionMissing() throws Exception {
      when(networkFetcher.get()).thenThrow(new IOException("no internet simulated exception"));

      final Optional<Version> version = liveServersFetcher.latestVersion();

      assertThat(version, isEmpty());
    }
  }

  @Nested
  class ServerForCurrentVersion {
    @Test
    @DisplayName("Verify fetching current version uses version selector")
    void serverForCurrentVersion() throws Exception {
      when(networkFetcher.get()).thenReturn(liveServers);
      when(currentVersionSelector.apply(liveServers)).thenReturn(serverProperties);

      final ServerProperties result = liveServersFetcher.serverForCurrentVersion();

      assertThat(result, sameInstance(serverProperties));
    }

    @Test
    @DisplayName("Verify fetch exceptions are translated to LobbyAddressFetchException")
    void serverForCurrentVersionExceptionCase() throws Exception {
      when(networkFetcher.get()).thenThrow(new IOException("simulated no network exception"));

      assertThrows(
          LobbyAddressFetchException.class, () -> liveServersFetcher.serverForCurrentVersion());
    }
  }

  @Nested
  class LobbyUriForCurrentVersion {
    @Test
    @DisplayName("Verify fetch current version is from current server properties selection")
    void lobbyUriForCurrentVersion() throws Exception {
      when(networkFetcher.get()).thenReturn(liveServers);
      when(currentVersionSelector.apply(liveServers)).thenReturn(serverProperties);
      when(serverProperties.getUri()).thenReturn(LOBBY_URI);

      final Optional<URI> result = liveServersFetcher.lobbyUriForCurrentVersion();

      assertThat(result, isPresentAndIs(LOBBY_URI));
    }

    @Test
    @DisplayName("Verify fetch current version returns empty if there is a fetch exception")
    void lobbyUriForCurrentVersionExceptionCase() throws Exception {
      when(networkFetcher.get()).thenThrow(new IOException("simulated no network exception"));

      final Optional<URI> result = liveServersFetcher.lobbyUriForCurrentVersion();

      assertThat(result, isEmpty());
    }
  }
}
