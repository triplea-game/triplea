package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadTest {
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private DownloadFileDescription mockDownload;

  @Test
  public void testBasicStartCancel() throws Exception {
    final DownloadFile testObj = new DownloadFile(mockDownload, e -> {
    }, () -> {
    });
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload(temporaryFolder.newFile());
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }
}
