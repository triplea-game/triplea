package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.Lists;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.MapDownloadListSort;
import games.strategy.util.Version;

public class MapDownloadListSortTest {

  private static final DownloadFileDescription MAP_A = createDownload("a", "url");
  private static final DownloadFileDescription MAP_B = createDownload("B", "url"); // capitol B to ensure case
                                                                                   // insensitive sorting
  private static final DownloadFileDescription MAP_C = createDownload("c", "url");
  private static final DownloadFileDescription MAP_D = createDownload("d", "url");
  private static final DownloadFileDescription HEADER = createDownload("header", DownloadFileDescription.DUMMY_URL);


  private static DownloadFileDescription createDownload(String mapName, String url) {
    String description = "fake";
    Version version = new Version("1");
    return new DownloadFileDescription(url, description, mapName, version, DownloadFileDescription.DownloadType.MAP);
  }

  @Test
  public void testSortingSortedList() {
    List<DownloadFileDescription> downloads = Lists.newArrayList(MAP_A, MAP_B, MAP_C);

    List<DownloadFileDescription> sorted = MapDownloadListSort.sortByMapName(downloads);
    assertSorted(sorted);
  }

  private static void assertSorted(List<DownloadFileDescription> sorted) {
    assertThat(sorted.get(0).getMapName(), is(MAP_A.getMapName()));
    assertThat(sorted.get(1).getMapName(), is(MAP_B.getMapName()));
    assertThat(sorted.get(2).getMapName(), is(MAP_C.getMapName()));
  }

  @Test
  public void testSortingUnSortedList() {
    List<DownloadFileDescription> downloads = Lists.newArrayList(MAP_B, MAP_C, MAP_A);
    List<DownloadFileDescription> sorted = MapDownloadListSort.sortByMapName(downloads);
    assertSorted(sorted);
  }

  @Test
  public void testInsertEmptyRowAboveHeaders() {
    List<DownloadFileDescription> downloads =
        Lists.newArrayList(HEADER, HEADER);
    List<DownloadFileDescription> sorted = MapDownloadListSort.sortByMapName(downloads);
    assertThat(sorted.size(), is(downloads.size() + 1));
    assertThat(sorted.get(0).isDummyUrl(), is(true));
    assertThat(sorted.get(1).isDummyUrl(), is(true));
    assertThat(sorted.get(2).isDummyUrl(), is(true));
  }

  @Test
  public void testSortingUnSortedListWithHeaders() {
    List<DownloadFileDescription> downloads =
        Lists.newArrayList(HEADER, MAP_B, MAP_A, HEADER, MAP_D, MAP_C, HEADER, HEADER);
    List<DownloadFileDescription> sorted = MapDownloadListSort.sortByMapName(downloads);
    assertThat(sorted.get(0).isDummyUrl(), is(true));
    assertThat(sorted.get(1).getMapName(), is(MAP_A.getMapName()));
    assertThat(sorted.get(2).getMapName(), is(MAP_B.getMapName()));
    assertThat(sorted.get(3).isDummyUrl(), is(true));
    assertThat(sorted.get(4).isDummyUrl(), is(true));
    assertThat(sorted.get(5).getMapName(), is(MAP_C.getMapName()));
    assertThat(sorted.get(6).getMapName(), is(MAP_D.getMapName()));
    assertThat(sorted.get(7).isDummyUrl(), is(true));
    assertThat(sorted.get(8).isDummyUrl(), is(true));
    assertThat(sorted.get(9).isDummyUrl(), is(true));
    assertThat(sorted.get(10).isDummyUrl(), is(true));
  }
}
