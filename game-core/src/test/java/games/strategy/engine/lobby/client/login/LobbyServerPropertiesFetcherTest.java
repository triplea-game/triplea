package games.strategy.engine.lobby.client.login;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import games.strategy.triplea.settings.GameSetting;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.OptionalUtils;
import org.triplea.java.function.ThrowingFunction;
import org.triplea.util.Version;

@ExtendWith(MockitoExtension.class)
class LobbyServerPropertiesFetcherTest {
  @Mock
  private BiFunction<
          String,
          ThrowingFunction<InputStream, LobbyServerProperties, IOException>,
          Optional<LobbyServerProperties>>
      mockFileDownloader;

  private LobbyServerPropertiesFetcher testObj;

  @BeforeEach
  void setup() {
    testObj = new LobbyServerPropertiesFetcher(mockFileDownloader);
  }

  @Nested
  final class DownloadAndParseRemoteFileTest {
    /** Happy case test path, download file, parse it, return values parsed. */
    @Test
    void happyCase() {
      when(mockFileDownloader.apply(eq(TestData.url), any()))
          .thenReturn(Optional.of(TestData.lobbyServerProperties));

      final Optional<LobbyServerProperties> result =
          testObj.downloadAndParseRemoteFile(TestData.url, TestData.version, (a, b) -> null);

      assertThat(result, isPresentAndIs(TestData.lobbyServerProperties));
    }

    @Test
    void throwsOnDownloadFailure() {
      when(mockFileDownloader.apply(eq(TestData.url), any())).thenReturn(Optional.empty());

      final Optional<LobbyServerProperties> result =
          testObj.downloadAndParseRemoteFile(TestData.url, TestData.version, (a, b) -> null);

      assertThat(result, isEmpty());
    }
  }

  @Nested
  final class GetTestOverridePropertiesTest {
    @Mock private GameSetting<String> testLobbyHostSetting;

    @Mock private GameSetting<Integer> testLobbyPortSetting;

    @Mock private GameSetting<Integer> testLobbyHttpsPort;

    private Optional<LobbyServerProperties> result;

    private void givenTestLobbyHostIsNotSet() {
      when(testLobbyHostSetting.isSet()).thenReturn(false);
    }

    private void givenTestLobbyHttpUriIsSet() {
      when(testLobbyHttpsPort.getValue()).thenReturn(Optional.of(999));
    }

    private void givenTestLobbyHostIsSetTo(final String host) {
      when(testLobbyHostSetting.isSet()).thenReturn(true);
      when(testLobbyHostSetting.getValueOrThrow()).thenReturn(host);
    }

    private void givenTestLobbyPortIsSetTo(final int port) {
      when(testLobbyPortSetting.getValue()).thenReturn(Optional.of(port));
    }

    private void whenGetTestOverrideProperties() {
      result =
          LobbyServerPropertiesFetcher.getTestOverrideProperties(
              testLobbyHostSetting, testLobbyPortSetting, testLobbyHttpsPort);
    }

    private void thenResultIsEmpty() {
      assertThat(result, is(Optional.empty()));
    }

    private void thenResultHasHostAndPort(final String host, final int port) {
      OptionalUtils.ifPresentOrElse(
          result,
          it -> {
            assertThat(it.getHost(), is(host));
            assertThat(it.getPort(), is(port));
          },
          () -> fail("expected non-empty properties, but was empty"));
    }

    @Test
    void shouldReturnEmptyWhenHostNotSetAndPortNotSet() {
      givenTestLobbyHostIsNotSet();

      whenGetTestOverrideProperties();

      thenResultIsEmpty();
    }

    @Test
    void shouldReturnDefaultPortWhenHostIsSetAndPortNotSet() {
      givenTestLobbyHostIsSetTo("foo");

      whenGetTestOverrideProperties();

      thenResultHasHostAndPort("foo", LobbyServerPropertiesFetcher.TEST_LOBBY_DEFAULT_PORT);
    }

    @Test
    void shouldReturnEmptyWhenHostNotSetAndPortSet() {
      givenTestLobbyHostIsNotSet();

      whenGetTestOverrideProperties();

      thenResultIsEmpty();
    }

    @Test
    void shouldReturnPropertiesWhenHostSetAndPortSet() {
      givenTestLobbyHostIsSetTo("foo");
      givenTestLobbyPortIsSetTo(4242);
      givenTestLobbyHttpUriIsSet();

      whenGetTestOverrideProperties();

      thenResultHasHostAndPort("foo", 4242);
    }
  }

  private interface TestData {
    Version version = new Version("0.0.0.0");
    String url = "someUrl";
    LobbyServerProperties lobbyServerProperties =
        LobbyServerProperties.builder().host("host").port(123).httpsPort(333).build();
  }
}
