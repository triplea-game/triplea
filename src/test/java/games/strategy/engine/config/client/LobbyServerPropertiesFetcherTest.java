package games.strategy.engine.config.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.engine.framework.map.download.DownloadUtils;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.triplea.settings.GameSetting;
import games.strategy.util.OptionalUtils;
import games.strategy.util.Version;

@ExtendWith(MockitoExtension.class)
public class LobbyServerPropertiesFetcherTest {
  @Mock
  private LobbyLocationFileDownloader mockFileDownloader;

  private LobbyServerPropertiesFetcher testObj;

  @BeforeEach
  public void setup() {
    testObj = new LobbyServerPropertiesFetcher(mockFileDownloader);
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class DownloadAndParseRemoteFileTest {
    /**
     * Happy case test path, download file, parse it, return values parsed.
     */
    @Test
    public void happyCase() throws Exception {
      givenHappyCase();

      final LobbyServerProperties result = testObj.downloadAndParseRemoteFile(TestData.url,
          TestData.version, (a, b) -> TestData.lobbyServerProperties);

      assertThat(result, sameInstance(TestData.lobbyServerProperties));
    }

    private void givenHappyCase() throws Exception {
      final File temp = File.createTempFile("temp", "tmp");
      temp.deleteOnExit();
      when(mockFileDownloader.download(TestData.url)).thenReturn(DownloadUtils.FileDownloadResult.success(temp));
    }

    @Test
    public void throwsOnDownloadFailure() {
      assertThrows(IOException.class, () -> {
        when(mockFileDownloader.download(TestData.url)).thenReturn(DownloadUtils.FileDownloadResult.FAILURE);

        testObj.downloadAndParseRemoteFile(TestData.url, TestData.version, (a, b) -> TestData.lobbyServerProperties);
      });
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class GetTestOverridePropertiesTest {
    @Mock
    private GameSetting testLobbyHostSetting;

    @Mock
    private GameSetting testLobbyPortSetting;

    private Optional<LobbyServerProperties> result;

    private void givenTestLobbyHostIsNotSet() {
      when(testLobbyHostSetting.isSet()).thenReturn(false);
    }

    private void givenTestLobbyHostIsSet() {
      givenTestLobbyHostIsSetTo("host");
    }

    private void givenTestLobbyHostIsSetTo(final String host) {
      when(testLobbyHostSetting.isSet()).thenReturn(true);
      when(testLobbyHostSetting.value()).thenReturn(host);
    }

    private void givenTestLobbyPortIsNotSet() {
      when(testLobbyPortSetting.isSet()).thenReturn(false);
    }

    private void givenTestLobbyPortIsSet() {
      givenTestLobbyPortIsSetTo(0);
    }

    private void givenTestLobbyPortIsSetTo(final int port) {
      when(testLobbyPortSetting.isSet()).thenReturn(true);
      when(testLobbyPortSetting.intValue()).thenReturn(port);
    }

    private void whenGetTestOverrideProperties() {
      result = LobbyServerPropertiesFetcher.getTestOverrideProperties(testLobbyHostSetting, testLobbyPortSetting);
    }

    private void thenResultIsEmpty() {
      assertThat(result, is(Optional.empty()));
    }

    private void thenResultHasHostAndPort(final String host, final int port) {
      OptionalUtils.ifPresentOrElse(
          result,
          it -> {
            assertThat(it.host, is(host));
            assertThat(it.port, is(port));
          },
          () -> fail("expected non-empty properties, but was empty"));
    }

    @Test
    public void shouldReturnEmptyWhenHostNotSetAndPortNotSet() {
      givenTestLobbyHostIsNotSet();
      givenTestLobbyPortIsNotSet();

      whenGetTestOverrideProperties();

      thenResultIsEmpty();
    }

    @Test
    public void shouldReturnEmptyWhenHostSetAndPortNotSet() {
      givenTestLobbyHostIsSet();
      givenTestLobbyPortIsNotSet();

      whenGetTestOverrideProperties();

      thenResultIsEmpty();
    }

    @Test
    public void shouldReturnEmptyWhenHostNotSetAndPortSet() {
      givenTestLobbyHostIsNotSet();
      givenTestLobbyPortIsSet();

      whenGetTestOverrideProperties();

      thenResultIsEmpty();
    }

    @Test
    public void shouldReturnPropertiesWhenHostSetAndPortSet() {
      givenTestLobbyHostIsSetTo("foo");
      givenTestLobbyPortIsSetTo(4242);

      whenGetTestOverrideProperties();

      thenResultHasHostAndPort("foo", 4242);
    }
  }

  private interface TestData {
    Version version = new Version("0.0.0.0");
    String url = "someUrl";
    LobbyServerProperties lobbyServerProperties = new LobbyServerProperties("host", 123);
  }
}
