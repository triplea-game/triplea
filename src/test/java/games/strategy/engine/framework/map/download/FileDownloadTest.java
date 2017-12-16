package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;

@ExtendWith(MockitoExtension.class)
public class FileDownloadTest {
  @Mock
  private DownloadFileDescription mockDownload;

  @Test
  public void testBasicStartCancel() {
    final DownloadFile testObj = new DownloadFile(mockDownload, mock(DownloadListener.class));
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }
}
