package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.util.Version;

@ExtendWith(MockitoExtension.class)
class MapDownloadListTest extends AbstractClientSettingTestCase {
  private static final String MAP_NAME = "new_test_order";
  private static final Version MAP_VERSION = new Version(10, 10, 0);
  private static final Version lowVersion = new Version(0, 0, 0);

  private static final DownloadFileDescription TEST_MAP =
      new DownloadFileDescription(
          "",
          "",
          MAP_NAME,
          MAP_VERSION,
          DownloadFileDescription.DownloadType.MAP,
          DownloadFileDescription.MapCategory.EXPERIMENTAL,
          "");

  @Mock private FileSystemAccessStrategy strategy;

  private final List<DownloadFileDescription> descriptions = new ArrayList<>();

  @BeforeEach
  void setUp() {
    descriptions.add(TEST_MAP);
  }

  @Test
  void testAvailable() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.empty());
    final MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    final List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(1));

    final List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(0));

    final List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(0));
  }

  @Test
  void testAvailableExcluding() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.empty());
    final DownloadFileDescription download1 = newDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newDownloadWithUrl("url3");
    final MapDownloadList testObj =
        new MapDownloadList(List.of(download1, download2, download3), strategy);

    final List<DownloadFileDescription> available =
        testObj.getAvailableExcluding(List.of(download1, download3));

    assertThat(available, is(List.of(download2)));
  }

  private static DownloadFileDescription newDownloadWithUrl(final String url) {
    return new DownloadFileDescription(
        url,
        "description",
        "mapName",
        MAP_VERSION,
        DownloadFileDescription.DownloadType.MAP,
        DownloadFileDescription.MapCategory.BEST,
        "");
  }

  @Test
  void testInstalled() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(MAP_VERSION));
    final MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    final List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(0));

    final List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(1));

    final List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(0));
  }

  @Test
  void testOutOfDate() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(lowVersion));
    final MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    final List<DownloadFileDescription> available = testObj.getAvailable();
    assertThat(available.size(), is(0));

    final List<DownloadFileDescription> installed = testObj.getInstalled();
    assertThat(installed.size(), is(1));

    final List<DownloadFileDescription> outOfDate = testObj.getOutOfDate();
    assertThat(outOfDate.size(), is(1));
  }

  @Test
  void testOutOfDateExcluding() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(lowVersion));
    final DownloadFileDescription download1 = newDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newDownloadWithUrl("url3");
    final MapDownloadList testObj =
        new MapDownloadList(List.of(download1, download2, download3), strategy);

    final List<DownloadFileDescription> outOfDate =
        testObj.getOutOfDateExcluding(List.of(download1, download3));

    assertThat(outOfDate, is(List.of(download2)));
  }

  @Test
  void testIsInstalled() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(MAP_VERSION));
    final MapDownloadList testObj = new MapDownloadList(descriptions, strategy);

    assertThat(testObj.isInstalled(TEST_MAP), is(true));
    assertThat(testObj.isInstalled(newDownloadWithUrl("url1")), is(false));
  }
}
