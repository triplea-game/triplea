package games.strategy.engine.framework.map.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import games.strategy.util.Version;

public class MapDownloadListSortTest {

  private static final DownloadFileDescription MAP_A = createDownload("a", "url");
  private static final DownloadFileDescription MAP_B = createDownload("B", "url"); // capitol B to ensure case
                                                                                   // insensitive sorting
  private static final DownloadFileDescription MAP_C = createDownload("c", "url");
  private static final DownloadFileDescription MAP_D = createDownload("d", "url");
  private static final DownloadFileDescription HEADER = createDownload("header", null);


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
    assertEquals(sorted.get(0).getMapName(), MAP_A.getMapName());
    assertEquals(sorted.get(1).getMapName(), MAP_B.getMapName());
    assertEquals(sorted.get(2).getMapName(), MAP_C.getMapName());
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
    assertEquals(sorted.size(), downloads.size() + 1);
    assertTrue(sorted.get(0).isDummyUrl());
    assertTrue(sorted.get(1).isDummyUrl());
    assertTrue(sorted.get(2).isDummyUrl());
  }

  @Test
  public void testSortingUnSortedListWithHeaders() {
    List<DownloadFileDescription> downloads =
        Lists.newArrayList(HEADER, MAP_B, MAP_A, HEADER, MAP_D, MAP_C, HEADER, HEADER);
    List<DownloadFileDescription> sorted = MapDownloadListSort.sortByMapName(downloads);
    assertTrue(sorted.get(0).isDummyUrl());
    assertEquals(sorted.get(1).getMapName(), MAP_A.getMapName());
    assertEquals(sorted.get(2).getMapName(), MAP_B.getMapName());
    assertTrue(sorted.get(3).isDummyUrl());
    assertTrue(sorted.get(4).isDummyUrl());
    assertEquals(sorted.get(5).getMapName(), MAP_C.getMapName());
    assertEquals(sorted.get(6).getMapName(), MAP_D.getMapName());
    assertTrue(sorted.get(7).isDummyUrl());
    assertTrue(sorted.get(8).isDummyUrl());
    assertTrue(sorted.get(9).isDummyUrl());
    assertTrue(sorted.get(10).isDummyUrl());
  }
}
