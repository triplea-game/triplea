package games.strategy.engine.config.client;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.engine.framework.map.download.DownloadUtils;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.util.Version;

@ExtendWith(MockitoExtension.class)
public class LobbyServerPropertiesFetcherTest {

  private static final Version fakeVersion = new Version("0.0.0.0");

  @Mock
  private LobbyLocationFileDownloader mockFileDownloader;

  private LobbyServerPropertiesFetcher testObj;

  /**
   * Sets up a test object with mocked dependencies. We will primarily verify control flow.
   */
  @BeforeEach
  public void setup() {
    testObj = new LobbyServerPropertiesFetcher(mockFileDownloader);
  }

  /**
   * Happy case test path, download file, parse it, return values parsed.
   */
  @Test
  public void downloadAndParseRemoteFile() throws Exception {
    givenHappyCase();

    final LobbyServerProperties result = testObj.downloadAndParseRemoteFile(TestData.url,
        fakeVersion, (a, b) -> TestData.lobbyServerProperties);

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
      final File temp = File.createTempFile("temp", "tmp");
      temp.deleteOnExit();
      when(mockFileDownloader.download(TestData.url)).thenReturn(DownloadUtils.FileDownloadResult.FAILURE);

      testObj.downloadAndParseRemoteFile(TestData.url, fakeVersion, (a, b) -> TestData.lobbyServerProperties);
    });
  }


  private interface TestData {
    String url = "someUrl";
    LobbyServerProperties lobbyServerProperties = new LobbyServerProperties("host", 123);
  }
}
