package games.strategy.engine.lobby.client.login;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.triplea.settings.GameSetting;
import games.strategy.util.OptionalUtils;
import games.strategy.util.Version;

@ExtendWith(MockitoExtension.class)
class LobbyServerPropertiesFetcherTest {
  @Mock
  private Function<String, Optional<File>> mockFileDownloader;

  private LobbyServerPropertiesFetcher testObj;

  @BeforeEach
  void setup() {
    testObj = new LobbyServerPropertiesFetcher(mockFileDownloader);
  }

  @Nested
  final class DownloadAndParseRemoteFileTest {
    /**
     * Happy case test path, download file, parse it, return values parsed.
     */
    @Test
    void happyCase() throws Exception {
      givenHappyCase();

      final Optional<LobbyServerProperties> result = testObj.downloadAndParseRemoteFile(TestData.url,
          TestData.version, (a, b) -> TestData.lobbyServerProperties);

      assertThat(result, isPresentAndIs(TestData.lobbyServerProperties));
    }

    private void givenHappyCase() throws Exception {
      final File temp = File.createTempFile("temp", "tmp");
      temp.deleteOnExit();
      when(mockFileDownloader.apply(TestData.url)).thenReturn(Optional.of(temp));
    }

    @Test
    void throwsOnDownloadFailure() {
      when(mockFileDownloader.apply(TestData.url)).thenReturn(Optional.empty());

      final Optional<LobbyServerProperties> result =
          testObj.downloadAndParseRemoteFile(TestData.url, TestData.version, (a, b) -> TestData.lobbyServerProperties);

      assertThat(result, isEmpty());
    }
  }

  @Nested
  final class GetTestOverridePropertiesTest {
    @Mock
    private GameSetting<String> testLobbyHostSetting;

    @Mock
    private GameSetting<Integer> testLobbyPortSetting;

    @Mock
    private GameSetting<String> testLobbyHttpUriSetting;

    private Optional<LobbyServerProperties> result;

    private void givenTestLobbyHostIsNotSet() {
      when(testLobbyHostSetting.isSet()).thenReturn(false);
    }

    private void givenTestLobbyHostIsSet() {
      when(testLobbyHostSetting.isSet()).thenReturn(true);
    }

    private void givenTestLobbyHttpUriIsSet() {
      when(testLobbyHttpUriSetting.isSet()).thenReturn(true);
      when(testLobbyHttpUriSetting.getValueOrThrow()).thenReturn("http://uri");
    }

    private void givenTestLobbyHostIsSetTo(final String host) {
      when(testLobbyHostSetting.isSet()).thenReturn(true);
      when(testLobbyHostSetting.getValueOrThrow()).thenReturn(host);
    }

    private void givenTestLobbyPortIsNotSet() {
      when(testLobbyPortSetting.isSet()).thenReturn(false);
    }

    private void givenTestLobbyPortIsSetTo(final int port) {
      when(testLobbyPortSetting.isSet()).thenReturn(true);
      when(testLobbyPortSetting.getValueOrThrow()).thenReturn(port);
    }

    private void whenGetTestOverrideProperties() {
      result =
          LobbyServerPropertiesFetcher.getTestOverrideProperties(
              testLobbyHostSetting, testLobbyPortSetting, testLobbyHttpUriSetting);
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
    void shouldReturnEmptyWhenHostSetAndPortNotSet() {
      givenTestLobbyHostIsSet();
      givenTestLobbyPortIsNotSet();

      whenGetTestOverrideProperties();

      thenResultIsEmpty();
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
        LobbyServerProperties.builder()
            .host("host")
            .port(123)
            .httpServerUri(URI.create("http://demo"))
            .build();
  }
}
