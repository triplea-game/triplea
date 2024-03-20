package org.triplea.maps.indexing;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.maps.MapsModuleDatabaseTestSupport;

@AllArgsConstructor
@DataSet(value = "map_index.yml", useSequenceFiltering = false)
@ExtendWith(MapsModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
class MapIndexDaoTest {

  private final MapIndexDao mapIndexDao;

  @Test
  @ExpectedDataSet(value = "expected/map_index_upsert_updated.yml", orderBy = "id")
  void upsertUpdatesRecords() {
    mapIndexDao.upsert(
        MapIndexingResult.builder()
            .mapName("map-name-updated")
            .mapRepoUri("http-map-repo-url-2")
            .lastCommitDate(LocalDateTime.of(2000, 1, 12, 23, 59).toInstant(ZoneOffset.UTC))
            .mapDownloadSizeInBytes(6789L)
            .downloadUri("http-map-repo-3-download-updated-url")
            .previewImageUri("http-preview-image-url-3")
            .description("description-updated")
            .build());
  }

  /** The data we are inserting matches what is present in map_index.yml */
  @Test
  @ExpectedDataSet("map_index.yml")
  void upsertSameData() {
    mapIndexDao.upsert(
        MapIndexingResult.builder()
            .mapName("map-name-2")
            .mapRepoUri("http-map-repo-url-2")
            .lastCommitDate(LocalDateTime.of(2016, 1, 1, 23, 59, 20).toInstant(ZoneOffset.UTC))
            .mapDownloadSizeInBytes(1000L)
            .downloadUri("http-map-repo-url-2/archives/master.zip")
            .previewImageUri("http-preview-image-url-2")
            .description("description-repo-2")
            .build());
  }

  @Test
  @ExpectedDataSet("expected/map_index_post_remove.yml")
  void removeMaps() {
    mapIndexDao.removeMapsNotIn(List.of("http-map-repo-url"));
  }

  @Test
  void getLastCommitDate() {
    assertThat(
        mapIndexDao.getLastCommitDate("http-map-repo-url"),
        isPresentAndIs(LocalDateTime.of(2000, 12, 1, 23, 59, 20).toInstant(ZoneOffset.UTC)));
    assertThat(
        mapIndexDao.getLastCommitDate("http-map-repo-url-2"),
        isPresentAndIs(LocalDateTime.of(2016, 1, 1, 23, 59, 20).toInstant(ZoneOffset.UTC)));

    assertThat(
        "Map repo URL does not exist",
        mapIndexDao.getLastCommitDate("http://map-repo-url-DNE"),
        OptionalMatchers.isEmpty());
  }
}
