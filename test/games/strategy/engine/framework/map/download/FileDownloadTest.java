package games.strategy.engine.framework.map.download;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;


@RunWith(MockitoJUnitRunner.class)
public class FileDownloadTest {

  @Mock
  private DownloadFileDescription mockDownload;

  @Test
  public void testBasicStartCancel() {
    final DownloadFile testObj = new DownloadFile(mockDownload, e -> {
    }, () -> {
    });
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }
}
