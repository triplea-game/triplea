package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.maps.listing.MapDownloadListing;

@ExtendWith(MockitoExtension.class)
class MapsListingModuleTest {

  @Mock private MapListingDao mapListingDao;

  @InjectMocks private MapsListingModule mapsListingModule;

  @Test
  @DisplayName("Should call map listing DAOs, merge and return data")
  void verifyDataFetch() {
    when(mapListingDao.fetchMapListings())
        .thenReturn(
            List.of(
                MapListingRecord.builder()
                    .id(1)
                    .url("http://map-url-1")
                    .name("map-name-1")
                    .description("description-1")
                    .version("1")
                    .categoryName("category-1")
                    .previewImageUrl("http://preview-url-1")
                    .build(),
                MapListingRecord.builder()
                    .id(2)
                    .url("http://map-url-2")
                    .name("map-name-2")
                    .description("description-2")
                    .version("2")
                    .categoryName("category-2")
                    .previewImageUrl("http://preview-url-2")
                    .build()));

    final List<MapDownloadListing> results = mapsListingModule.get();
    assertThat(results, hasSize(2));
    // expected sort by map name, so first map should be id "1"
    assertThat(results.get(0).getMapName(), is("map-name-1"));
    assertThat(results.get(0).getPreviewImage(), is("http://preview-url-1"));
    assertThat(results.get(0).getVersion(), is("1"));
    assertThat(results.get(0).getUrl(), is("http://map-url-1"));
    assertThat(results.get(0).getDescription(), is("description-1"));
    assertThat(results.get(0).getMapCategory(), is("category-1"));

    assertThat(results.get(1).getMapName(), is("map-name-2"));
    assertThat(results.get(1).getPreviewImage(), is("http://preview-url-2"));
    assertThat(results.get(1).getVersion(), is("2"));
    assertThat(results.get(1).getUrl(), is("http://map-url-2"));
    assertThat(results.get(1).getDescription(), is("description-2"));
    assertThat(results.get(1).getMapCategory(), is("category-2"));
  }
}
