package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.maps.MapsModuleDatabaseTestSupport;

@DataSet(value = "map_index.yml,map_tag_values.yml", useSequenceFiltering = false)
@ExtendWith(MapsModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
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
    assertThat(mapDownloadListing.getPreviewImageUrl(), is("http-preview-image-url"));
    assertThat(mapDownloadListing.getDescription(), is("description-repo-1"));
    assertThat(
        mapDownloadListing.getLastCommitDate().toEpochMilli(),
        is(LocalDateTime.of(2000, 12, 1, 23, 59, 20).toInstant(ZoneOffset.UTC).toEpochMilli()));
  }

  @Test
  void verifyFetchMapTags() {
    var mapTags =
        mapListingDao.fetchMapTagsForMapName("map-name").stream()
            .map(MapTagRecord::toMapTag)
            .collect(Collectors.toList());
    assertThat(mapTags, hasSize(2));
    assertThat(mapTags.get(0).getName(), is("Category"));
    assertThat(mapTags.get(0).getType(), is("STRING"));
    assertThat(mapTags.get(0).getDisplayOrder(), is(1));
    assertThat(mapTags.get(0).getValue(), is("Best"));

    assertThat(mapTags.get(1).getName(), is("Rating"));
    assertThat(mapTags.get(1).getType(), is("STAR"));
    assertThat(mapTags.get(1).getDisplayOrder(), is(2));
    assertThat(mapTags.get(1).getValue(), is("5"));

    mapTags =
        mapListingDao.fetchMapTagsForMapName("map-name-2").stream()
            .map(MapTagRecord::toMapTag)
            .collect(Collectors.toList());
    assertThat(mapTags, hasSize(1));
    assertThat(mapTags.get(0).getName(), is("Category"));
    assertThat(mapTags.get(0).getType(), is("STRING"));
    assertThat(mapTags.get(0).getDisplayOrder(), is(1));
    assertThat(mapTags.get(0).getValue(), is("New"));
  }
}
