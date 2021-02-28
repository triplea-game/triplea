package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import org.junit.jupiter.api.Test;
import org.triplea.maps.server.http.MapServerTest;

@DataSet(value = "map_category.yml,map_index.yml", useSequenceFiltering = false)
class MapListingDaoTest extends MapServerTest {

  private final MapListingDao mapListingDao;

  MapListingDaoTest(final MapListingDao mapListingDao) {
    this.mapListingDao = mapListingDao;
  }

  @Test
  void verifySelect() {

    final var results = mapListingDao.fetchMapListings();

    assertThat(results, hasSize(2));
    final var mapDownloadListing = results.get(0).toMapDownloadListing();
    assertThat(mapDownloadListing.getMapCategory(), is("category_name"));
    assertThat(mapDownloadListing.getMapName(), is("map-name"));
    assertThat(mapDownloadListing.getUrl(), is("http://map-repo-url"));
    assertThat(mapDownloadListing.getVersion(), is("100"));
  }
}
