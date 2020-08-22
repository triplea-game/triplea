package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.util.Version;

class MapDownloadListSortTest {

  private static final DownloadFileDescription MAP_A = newDownload("a", "url");
  // capitol B to ensure case insensitive sorting
  private static final DownloadFileDescription MAP_B = newDownload("B", "url");
  private static final DownloadFileDescription MAP_C = newDownload("c", "url");

  private static DownloadFileDescription newDownload(final String mapName, final String url) {
    final String description = "fake";
    final Version version = new Version("1");
    return new DownloadFileDescription(
        url,
        description,
        mapName,
        version,
        DownloadFileDescription.DownloadType.MAP,
        DownloadFileDescription.MapCategory.EXPERIMENTAL,
        "");
  }

  @Test
  void testSortingSortedList() {
    final List<DownloadFileDescription> downloads = Lists.newArrayList(MAP_A, MAP_B, MAP_C);

    final List<DownloadFileDescription> sorted = MapDownloadListSort.sortByMapName(downloads);
    assertSorted(sorted);
  }

  private static void assertSorted(final List<DownloadFileDescription> sorted) {
    assertThat(sorted.get(0).getMapName(), is(MAP_A.getMapName()));
    assertThat(sorted.get(1).getMapName(), is(MAP_B.getMapName()));
    assertThat(sorted.get(2).getMapName(), is(MAP_C.getMapName()));
  }

  @Test
  void testSortingUnSortedList() {
    final List<DownloadFileDescription> downloads = Lists.newArrayList(MAP_B, MAP_C, MAP_A);
    final List<DownloadFileDescription> sorted = MapDownloadListSort.sortByMapName(downloads);
    assertSorted(sorted);
  }
}
