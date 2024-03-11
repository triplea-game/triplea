package org.triplea.maps;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lib.ClientIdentifiers;
import org.triplea.http.client.maps.listing.MapsClient;

/** Make sure endpoints are responding with seemingly valid data. */
@Slf4j
public class MapsListingTest {
  static final MapsClient client =
      MapsClient.newClient(
          URI.create("http://localhost:" + System.getenv("MAPS-SERVER_1_TCP_8080")),
          ClientIdentifiers.builder() //
              .applicationVersion("1.0")
              .apiKey("")
              .systemId("")
              .build());

  @Test
  void fetchMapListing() {
    var result = client.fetchMapListing();

    result.forEach(
        listing -> {
          assertThat(listing.getDescription()).isNotNull();
          assertThat(listing.getDescription()).isNotNull();
          assertThat(listing.getDownloadSizeInBytes()).isNotNull();
          assertThat(listing.getDownloadUrl()).isNotNull();
          assertThat(listing.getLastCommitDateEpochMilli()).isNotNull();
          assertThat(listing.getMapName()).isNotNull();
          assertThat(listing.getMapTags()).isNotNull();
        });
  }
}
