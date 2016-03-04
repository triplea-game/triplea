package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.FileSystemAccessStrategy;
import games.strategy.engine.framework.map.download.MapDownloadList;
import games.strategy.util.Version;


@RunWith(MockitoJUnitRunner.class)
public class MapDownloadListTest {


  private final static String MAP_NAME = "new_test_order";
  private final static Version MAP_VERSION = new Version(10, 10);
  private final static Version lowVersion = new Version(0, 0);

  private final static DownloadFileDescription TEST_MAP =
      new DownloadFileDescription("", "", MAP_NAME, MAP_VERSION, DownloadFileDescription.DownloadType.MAP);

  @Mock
  private FileSystemAccessStrategy strategy;

  private List<DownloadFileDescription> descriptions;

  @Before
  public void setUp() throws Exception {
    descriptions = Lists.newArrayList();
    descriptions.add(TEST_MAP);

  }


  @Test
  public void testAvailable() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.empty());
    MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(1));

    List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(0));

    List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(0));
  }

  @Test
  public void testInstalled() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(MAP_VERSION));
    MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(0));

    List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(1));

    List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(0));
  }

  @Test
  public void testOutOfDate() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(lowVersion));
    MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(0));

    List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(1));

    List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(1));
  }


}
