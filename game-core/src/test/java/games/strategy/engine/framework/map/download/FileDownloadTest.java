package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.triplea.util.Version;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;

class FileDownloadTest {
  @Test
  void testBasicStartCancel() {
    final DownloadFileDescription downloadFileDescription = new DownloadFileDescription(
        "url",
        "description",
        "mapName",
        new Version(0, 0),
        DownloadFileDescription.DownloadType.MAP,
        DownloadFileDescription.MapCategory.BEST);
    final DownloadFile testObj = new DownloadFile(downloadFileDescription, mock(DownloadListener.class));
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }
}
