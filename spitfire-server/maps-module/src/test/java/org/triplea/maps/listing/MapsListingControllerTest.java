package org.triplea.maps.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.maps.server.http.MapServerTest;

@DataSet(value = "map_category.yml,map_index.yml", useSequenceFiltering = false)
class MapsListingControllerTest extends MapServerTest {
  private static final Instant commitDate1 =
      LocalDateTime.of(2000, 12, 1, 23, 59, 20).toInstant(ZoneOffset.UTC);
  private static final Instant commitDate2 =
      LocalDateTime.of(2016, 1, 1, 23, 59, 20).toInstant(ZoneOffset.UTC);

  private final MapsListingClient mapsListingClient;

  MapsListingControllerTest(final URI serverUri) {
    mapsListingClient = new MapsListingClient(serverUri);
  }

  @Test
  void verifyMapListingEndpoint() {
    final List<MapDownloadListing> downloadListings = mapsListingClient.fetchMapDownloads();

    assertThat(downloadListings, hasSize(2));
    assertThat(downloadListings.get(0).getMapName(), is("map-name"));
    assertThat(
        downloadListings.get(0).getLastCommitDateEpochMilli(), is(commitDate1.toEpochMilli()));
    assertThat(downloadListings.get(0).getUrl(), is("http://map-repo-url"));
    assertThat(downloadListings.get(0).getMapCategory(), is("category_name"));

    assertThat(downloadListings.get(1).getMapName(), is("map-name-2"));
    assertThat(
        downloadListings.get(1).getLastCommitDateEpochMilli(), is(commitDate2.toEpochMilli()));
    assertThat(downloadListings.get(1).getUrl(), is("http://map-repo-url-2"));
    assertThat(downloadListings.get(1).getMapCategory(), is("category_name"));
  }
}
