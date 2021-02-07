package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;
import org.junit.jupiter.api.Test;

class DownloadFileTest {
  @Test
  void testBasicStartCancel() {
    final DownloadFileDescription downloadFileDescription =
        DownloadFileDescription.builder()
            .url("url")
            .description("description")
            .mapName("mapName")
            .version(0)
            .mapCategory(DownloadFileDescription.MapCategory.BEST)
            .build();
    final DownloadFile testObj =
        new DownloadFile(downloadFileDescription, mock(DownloadListener.class));
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }
}
