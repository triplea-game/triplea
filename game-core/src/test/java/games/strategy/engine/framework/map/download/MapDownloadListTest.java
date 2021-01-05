package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
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

@ExtendWith(MockitoExtension.class)
class MapDownloadListTest extends AbstractClientSettingTestCase {
  private static final String MAP_NAME = "new_test_order";
  private static final int MAP_VERSION = 10;
  private static final int lowVersion = 0;

  private static final DownloadFileDescription TEST_MAP =
      new DownloadFileDescription(
          "",
          "",
          MAP_NAME,
          MAP_VERSION,
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
    final MapDownloadList mapDownloadList = new MapDownloadList(descriptions, strategy);

    assertThat(mapDownloadList.getAvailable(), hasSize(1));
    assertThat(mapDownloadList.getInstalled(), is(empty()));
    assertThat(mapDownloadList.getOutOfDate(), is(empty()));
  }

  @Test
  void testAvailableExcluding() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.empty());
    final DownloadFileDescription download1 = newDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newDownloadWithUrl("url3");
    final MapDownloadList mapDownloadList =
        new MapDownloadList(List.of(download1, download2, download3), strategy);

    final List<DownloadFileDescription> available =
        mapDownloadList.getAvailableExcluding(List.of(download1, download3));

    assertThat(available, is(List.of(download2)));
  }

  private static DownloadFileDescription newDownloadWithUrl(final String url) {
    return new DownloadFileDescription(
        url,
        "description",
        "mapName" + url,
        MAP_VERSION,
        DownloadFileDescription.MapCategory.BEST,
        "");
  }

  @Test
  void testInstalled() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(MAP_VERSION));
    final MapDownloadList mapDownloadList = new MapDownloadList(descriptions, strategy);

    assertThat(mapDownloadList.getAvailable(), is(empty()));
    assertThat(mapDownloadList.getInstalled(), hasSize(1));
    assertThat(mapDownloadList.getOutOfDate(), is(empty()));
  }

  @Test
  void testOutOfDate() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(lowVersion));
    final MapDownloadList mapDownloadList = new MapDownloadList(descriptions, strategy);

    assertThat(mapDownloadList.getAvailable(), is(empty()));
    assertThat(mapDownloadList.getInstalled(), hasSize(1));
    assertThat(mapDownloadList.getOutOfDate(), hasSize(1));
  }

  @Test
  void testOutOfDateExcluding() {
    when(strategy.getMapVersion(any())).thenReturn(Optional.of(lowVersion));
    final DownloadFileDescription download1 = newDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newDownloadWithUrl("url3");
    final MapDownloadList mapDownloadList =
        new MapDownloadList(List.of(download1, download2, download3), strategy);

    final List<DownloadFileDescription> outOfDate =
        mapDownloadList.getOutOfDateExcluding(List.of(download1, download3));

    assertThat(outOfDate, is(List.of(download2)));
  }
}
