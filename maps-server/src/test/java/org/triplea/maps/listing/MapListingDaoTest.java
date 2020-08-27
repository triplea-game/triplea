package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.triplea.maps.server.http.MapServerTest;

@DataSet("map_listing/select_maps.yml")
class MapListingDaoTest extends MapServerTest {

  private final MapListingDao mapListingDao;

  MapListingDaoTest(final MapListingDao mapListingDao) {
    this.mapListingDao = mapListingDao;
  }

  @Test
  void verifySelect() {

    final var results = mapListingDao.fetchMapListings();

    assertThat(results, hasSize(1));
    final var mapDownloadListing = results.get(0).toMapDownloadListing(Set.of());
    assertThat(mapDownloadListing.getMapCategory(), is("category_name"));
    assertThat(mapDownloadListing.getDescription(), is("description of the map"));
    assertThat(mapDownloadListing.getPreviewImage(), is("http://map-thumbnail-url"));
    assertThat(mapDownloadListing.getMapName(), is("map-name"));
    assertThat(mapDownloadListing.getMapsSkins(), is(empty()));
    assertThat(mapDownloadListing.getUrl(), is("http://map-download-url"));
    assertThat(mapDownloadListing.getVersion(), is("100"));
  }
}
