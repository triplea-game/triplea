package games.strategy.engine.framework.map.download;


import static org.junit.Assert.assertEquals;

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
    DownloadFile testObj = new DownloadFile(mockDownload, e -> {}, () -> {});
    assertEquals(DownloadState.NOT_STARTED, testObj.getDownloadState());

    testObj.startAsyncDownload();
    assertEquals(DownloadState.DOWNLOADING, testObj.getDownloadState());

    testObj.cancelDownload();
    assertEquals(DownloadState.CANCELLED, testObj.getDownloadState());
  }
}
