package games.strategy.engine.config.client.remote;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.DownloadUtils;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.util.Version;

@RunWith(MockitoJUnitRunner.class)
public class RemoteFileFetcherTest {


  private static final Version fakeVersion = new Version("0.0.0.0");

  @Mock
  private UrlRedirectResolver mockUrlRedirectResolver;
  @Mock
  private LobbyPropertyFileParser mockLobbyPropertyFileParser;
  @Mock
  private FileDownloader mockFileDownloader;

  private RemoteFileFetcher testObj;

  /**
   * Sets up a test object with mocked dependencies. We will primarily verify control flow.
   */
  @Before
  public void setup() {
    testObj = new RemoteFileFetcher(
        mockUrlRedirectResolver,
        mockLobbyPropertyFileParser,
        mockFileDownloader);
  }

  /**
   * Happy case test path, download file, parse it, return values parsed.
   */
  @Test
  public void downloadAndParseRemoteFile() throws Exception {
    givenHappyCase();

    final LobbyServerProperties result = testObj.downloadAndParseRemoteFile(TestData.url, fakeVersion);

    assertThat(result, sameInstance(TestData.lobbyServerProperties));
  }

  private void givenHappyCase() throws Exception {
    when(mockUrlRedirectResolver.getUrlFollowingRedirects(TestData.url)).thenReturn(TestData.url);

    final File temp = File.createTempFile("temp", "tmp");
    temp.deleteOnExit();
    when(mockFileDownloader.download(TestData.url)).thenReturn(DownloadUtils.FileDownloadResult.success(temp));

    when(mockLobbyPropertyFileParser.parse(temp, fakeVersion)).thenReturn(TestData.lobbyServerProperties);
  }

  @Test(expected = IOException.class)
  public void throwsOnDownloadFailure() throws Exception {
    when(mockUrlRedirectResolver.getUrlFollowingRedirects(TestData.url)).thenReturn(TestData.url);

    final File temp = File.createTempFile("temp", "tmp");
    temp.deleteOnExit();
    when(mockFileDownloader.download(TestData.url)).thenReturn(DownloadUtils.FileDownloadResult.FAILURE);

    testObj.downloadAndParseRemoteFile(TestData.url, fakeVersion);
  }

  @Test(expected = IOException.class)
  public void throwsOnRedirectResolutionFailure() throws Exception {
    when(mockUrlRedirectResolver.getUrlFollowingRedirects(TestData.url))
        .thenThrow(new IOException("simulated failure"));
    testObj.downloadAndParseRemoteFile(TestData.url, fakeVersion);
  }

  private interface TestData {
    String url = "someUrl";
    LobbyServerProperties lobbyServerProperties = new LobbyServerProperties("host", 123);
  }
}
