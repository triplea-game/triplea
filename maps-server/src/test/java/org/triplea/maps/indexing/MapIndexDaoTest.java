package org.triplea.maps.indexing;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.List;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.triplea.maps.server.http.MapServerTest;

@AllArgsConstructor
@DataSet(value = "map_category.yml,map_index.yml", useSequenceFiltering = false)
class MapIndexDaoTest extends MapServerTest {

  private final MapIndexDao mapIndexDao;

  @Test
  @ExpectedDataSet(value="expected/map_index_upsert_new.yml", orderBy = "map_name")
  void upsertCreatesNewRecords() {
    mapIndexDao.upsert(
        MapIndexResult.builder()
            .mapName("map-name-3")
            .mapRepoUri("http://map-repo-url-3")
            .mapVersion(2)
            .build());
  }

  @Test
  @ExpectedDataSet(value="expected/map_index_upsert_updated.yml", orderBy = "id")
  void upsertUpdatesRecords() {
    mapIndexDao.upsert(
        MapIndexResult.builder()
            .mapName("map-name-updated")
            .mapRepoUri("http://map-repo-url-2")
            .mapVersion(1000)
            .build());
  }

  @Test
  @ExpectedDataSet("map_index.yml")
  void upsertSameData() {
    mapIndexDao.upsert(
        MapIndexResult.builder()
            .mapName("map-name-2")
            .mapRepoUri("http://map-repo-url-2")
            .mapVersion(100)
            .build());
  }

  @Test
  @ExpectedDataSet("expected/map_index_post_remove.yml")
  void removeMaps() {
    mapIndexDao.removeMapsNotIn(List.of("http://map-repo-url"));
  }
}
