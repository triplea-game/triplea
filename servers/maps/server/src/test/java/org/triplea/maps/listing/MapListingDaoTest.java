package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.maps.MapsModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@DataSet(value = "map_index.yml,map_tag_value.yml", useSequenceFiltering = false)
@ExtendWith(MapsModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class MapListingDaoTest {

  private final MapListingDao mapListingDao;

  MapListingDaoTest(final MapListingDao mapListingDao) {
    this.mapListingDao = mapListingDao;
  }

  @Test
  void verifySelect() {
    final var results = mapListingDao.fetchMapListings();
    assertThat(results, hasSize(2));
    final var mapDownloadListing = results.get(0);
    assertThat(mapDownloadListing.getName(), is("map-name"));
    assertThat(mapDownloadListing.getDownloadUrl(), is("http-map-repo-url/archives/master.zip"));
    assertThat(mapDownloadListing.getDownloadSizeBytes(), is(4000L));
    assertThat(mapDownloadListing.getPreviewImageUrl(), is("http-preview-image-url"));
    assertThat(mapDownloadListing.getDescription(), is("description-repo-1"));
    assertThat(
        mapDownloadListing.getLastCommitDate().toEpochMilli(),
        is(LocalDateTime.of(2000, 12, 1, 23, 59, 20).toInstant(ZoneOffset.UTC).toEpochMilli()));
  }
}
