package games.strategy.engine.framework.map.download;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.DownloadFile;
import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.MapDownloadStrategy;
import games.strategy.engine.framework.map.download.DownloadFile.DownloadState;


@RunWith(MockitoJUnitRunner.class)
public class FileDownloadTest {

  private final Consumer<Integer> fakeListener = e -> {
  };
  @Mock
  private MapDownloadStrategy downloadStrategy;

  @Mock
  private DownloadFileDescription mockDownload;

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testBasicStartCancel() {
    DownloadFile testObj = new DownloadFile(mockDownload, fakeListener,  () -> {});
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }
}
