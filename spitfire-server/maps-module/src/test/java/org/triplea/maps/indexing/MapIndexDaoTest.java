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
@DataSet(value = "map_category.yml,map_index.yml", useSequenceFiltering = false)
@ExtendWith(MapsModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
class MapIndexDaoTest {

  private final MapIndexDao mapIndexDao;

  @Test
  @ExpectedDataSet(value = "expected/map_index_upsert_new.yml", orderBy = "map_name")
  void upsertCreatesNewRecords() {
    mapIndexDao.upsert(
        MapIndexingResult.builder()
            .mapName("map-name-3")
            .mapRepoUri("http-map-repo-url-3")
            .lastCommitDate(LocalDateTime.of(2000, 1, 12, 23, 59).toInstant(ZoneOffset.UTC))
            .build());
  }

  @Test
  @ExpectedDataSet(value = "expected/map_index_upsert_updated.yml", orderBy = "id")
  void upsertUpdatesRecords() {
    mapIndexDao.upsert(
        MapIndexingResult.builder()
            .mapName("map-name-updated")
            .mapRepoUri("http-map-repo-url-2")
            .lastCommitDate(LocalDateTime.of(2000, 1, 12, 23, 59).toInstant(ZoneOffset.UTC))
            .build());
  }

  @Test
  @ExpectedDataSet("map_index.yml")
  void upsertSameData() {
    mapIndexDao.upsert(
        MapIndexingResult.builder()
            .mapName("map-name-2")
            .mapRepoUri("http-map-repo-url-2")
            .description("description-repo-2")
            .lastCommitDate(LocalDateTime.of(2016, 1, 1, 23, 59, 20).toInstant(ZoneOffset.UTC))
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
