package games.strategy.engine.framework.mapDownload;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.mapDownload.DownloadFile.DownloadState;


@RunWith(PowerMockRunner.class)
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

  @Mock
  private ClientContext mockContext;

  @Test
  public void testBasicStartCancel() {
    PowerMockito.mockStatic(ClientContext.class);
    BDDMockito.given(ClientContext.mapDownloadStrategy()).willReturn(downloadStrategy);

    DownloadFile testObj = new DownloadFile(mockDownload, fakeListener,  () -> {});
    assertThat(testObj.getDownloadState(), is(DownloadState.NOT_STARTED));

    testObj.startAsyncDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.DOWNLOADING));

    testObj.cancelDownload();
    assertThat(testObj.getDownloadState(), is(DownloadState.CANCELLED));
  }

}
